package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.alibaba.fastjson.JSON;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.netapi.*;
import com.sitech.crmpd.idmm.util.ZK;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gyf on 5/1/2017.
 */
public class CmdActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ActorRef reply;
    private ActorRef store;
    private ActorRef brk;
    private HashMap<Integer, Mem> parts = new HashMap<>();

    private int seq_seed = 0;
    private HashMap<Integer, Msg> toMem = new HashMap<>(); //发往MemActor的操作, 等待通知后应答Supervisor
//    private LinkedList<Mem> list = new LinkedList<>();

    // 保存最后发送消息给本BLE的supervisor的channel, 以便进行状态通知(leaving的分区消费完毕)
    private Channel supervisor;

    private ZK zk;

    /**
     * 接收的命令消息类型
     */
    public static class Msg {
        Channel channel;
        FramePacket packet;
        long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }
    public static class Notify{
        int seq;
        String s;
        public Notify(int seq, String s){
            this.seq = seq;
            this.s = s;
        }
    }
    public static class LeaveDone{
        String qid;
        int partid;
        public LeaveDone(String qid, int pid) {
            this.qid = qid;
            this.partid = pid;
        }
    }

    private static class Mem{
        PartConfig c;
        ActorRef ref;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Msg.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .match(RefMsg.class, s -> {
                    onReceive(s);
                })
                .match(Notify.class, s -> {
                    onReceive(s);
                })
                .match(LeaveDone.class, s -> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    /**
     * leave 分区消费完成通知, 来自MemActor, 用于向Supervisor 发起通知
     * @param s
     */
    private void onReceive(LeaveDone s) {
        FramePacket pr = new FramePacket(
                FrameType.CMD_PT_LEAVE_DONE,
                BMessage.c().p(BProps.QID, s.qid).p(BProps.PART_ID, s.partid),
                seq_seed++);
        supervisor.writeAndFlush(pr);
    }

    private void onReceive(RefMsg s){
        if("brk".equals(s.name))
            brk = s.ref;
        else if("store".equals(s.name)){
            store = s.ref;
        }else if("reply".equals(s.name)){
            reply = s.ref;
        }else if("zk".equals(s.name)) {
            zk = (ZK)s.obj;
        }
    }

    /**
     * 接收到从 MemActor 状态更改/启动完成的消息， 用于触发应答supervisor
      */
    private void onReceive(Notify n){
        Msg s = toMem.remove(n.seq);
        BMessage mr = null;
        //zk.chgPartStatus(s);
        if("ok".equals(n.s)) {
            mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
        }else{
            mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                .p(BProps.RESULT_DESC, "notify failed: "+n.s);
        }
        FramePacket p = s.packet;
        FramePacket pr = new FramePacket(
                FrameType.valueOfCode(p.getType().code() | 0x80),
                mr, p.getSeq());
        s.channel.writeAndFlush(pr);
    }

    /**
     * 处理从supervisor 过来的请求
     * @param s
     */
    private void onReceive(Msg s) {
        supervisor = s.channel;

        FramePacket p = s.packet;
        BMessage m = p.getMessage();
        BMessage mr = null;

        switch(p.getType() ) {
            case HEARTBEAT:
                mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
                break;
            case CMD_PT_START:
                log.info("CMD_PT_START, {}", System.currentTimeMillis());
                mr = startPart(s);
                break;
            case CMD_PT_CHANGE:
                log.info("CMD_PT_CHANGE, {}", System.currentTimeMillis());
                mr = chgPartStatus(s);
                break;
            case CMD_PT_QUERY:
                log.info("CMD_PT_QUERY, {}", System.currentTimeMillis());
                mr = query(m);
                break;
            case CMD_PT_LEAVE_DONE_ACK:
                log.info("leave done notify answered");
                break;
            default:
                log.error("unknown command {}", p.getType().code());
                break;
        }

        // 需要等待MemActor 通知完成后再应答

//        if(mr == null){
//            mr = BMessage.c()
//                    .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
//                    .p(BProps.RESULT_DESC, "invalid request");
//        }

        if(mr != null) {
            FramePacket pr = new FramePacket(
                    FrameType.valueOfCode(p.getType().code() | 0x80),
                    mr, p.getSeq());
            s.channel.writeAndFlush(pr);
        }
    }

    private BMessage query(BMessage m) {
        List<PartConfig> l = new LinkedList<>();
        for(Mem m1: parts.values()){
            l.add(m1.c);
        }
        String json = JSON.toJSON(l).toString();
        return BMessage.create(json).p(BProps.RESULT_CODE, RetCode.OK);
    }

    /**
     * 修改代码分区状态
     * @param s
     * @return
     */
    private BMessage chgPartStatus(Msg s) {
        FramePacket p = s.packet;
        BMessage m = p.getMessage();
        try {
            int part_id = m.p(BProps.PART_ID);
            PartConfig pc = JSONSerializable.fromJson(m.getContentAsString(), PartConfig.class);
            PartStatus status = pc.getStatus();
            if(!parts.containsKey(part_id)){
                return BMessage.c()
                        .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
                        .p(BProps.RESULT_DESC, "part not found");
            }
            Mem x = parts.get(part_id);
            x.c = pc;

            // 把分区的引用和配置都交给 brkActor
            brk.tell(new BrkActor.PartState(x.ref, pc), getSelf());

            int seq = seq_seed++;
            toMem.put(seq, s);
            x.ref.tell(new MemActor.PartChg(seq, pc), getSelf());
        }catch(NoSuchPropertyException ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }
        return null; //BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
    }

    private BMessage startPart(Msg s) {
        FramePacket f = s.packet;
        BMessage m = f.getMessage();
        // props: TARGET_TOPIC  CLIENT_ID， PART_NUM, PART_ID, PART_STATUS

        try {
            String body = m.getContentAsString();
            System.out.println("===="+body);
            PartConfig pc = JSONSerializable.fromJson(body, PartConfig.class);
            int part_id = pc.getPartId();

            if (parts.containsKey(pc.getPartId())) {
                return BMessage.c()
                        .p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                        .p(BProps.RESULT_DESC, "part_id " + pc.getPartId() + " dulplicated");
            }

            // 创建分区 actor
            int seq = seq_seed ++;
            toMem.put(seq, s);
            ActorRef mem = getContext().actorOf(
                    Props.create(MemActor.class, pc, store, brk, reply, getSelf(), seq),
                    String.valueOf(part_id));

            Mem x = new Mem();
            x.c = pc;
            x.ref = mem;
            parts.put(part_id, x);
            log.info("create part for:{}", mem.path().toString());

            // 把分区的引用和配置都交给 brkActor, 这里是由MemActor来通知的
            //brk.tell(new BrkActor.PartState(x.ref, pc), getSelf());

            return null; //BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
        }catch(NoSuchPropertyException ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }catch(Exception ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }
    }

}
