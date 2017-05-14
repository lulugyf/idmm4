package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.ble.util.SZK;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by gyf on 5/1/2017.
 */
public class CmdActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ActorRef reply;
    private ActorRef store;
    private ActorRef brk;
    private HashMap<Integer, Mem> parts = new HashMap<>();
    private LinkedList<Mem> list = new LinkedList<>();

    // 保存最后发送消息给本BLE的supervisor的channel, 以便进行状态通知(leaving的分区消费完毕)
    private Channel supervisor;

    private SZK zk;

    public static class Msg {
        Channel channel;
        FramePacket packet;
        long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
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
                .match(PartConfig.class, s -> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    private void onReceive(RefMsg s){
        if("brk".equals(s.name))
            brk = s.ref;
        else if("store".equals(s.name)){
            store = s.ref;
        }else if("reply".equals(s.name)){
            reply = s.ref;
        }else if("zk".equals(s.name)) {
            zk = (SZK)s.obj;
        }
    }

    private void onReceive(PartConfig s){
        zk.chgPartStatus(s);
    }

    private void onReceive(Msg s) {
        supervisor = s.channel;

        FramePacket p = s.packet;
        BMessage m = p.getMessage();
        BMessage mr = null;

        switch(p.getType() ) {
            case HEARTBEAT:
                break;
            case CMD_PT_START:
                log.info("CMD_PT_START, {}", System.currentTimeMillis());
                mr = startPart(m);
                break;
            case CMD_PT_CHANGE:
                log.info("CMD_PT_CHANGE, {}", System.currentTimeMillis());
                mr = chgPartStatus(m);
                break;
            case CMD_PT_QUERY:
                log.info("CMD_PT_QUERY, {}", System.currentTimeMillis());
                break;
            default:
                log.error("unknown command {}", p.getType().code());
                break;
        }

        if(mr == null){
            mr = BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
                    .p(BProps.RESULT_DESC, "invalid request");
        }

        FramePacket pr = new FramePacket(
                FrameType.valueOfCode(p.getType().code()|0x80),
                mr, p.getSeq());
        reply.tell( new ReplyActor.Msg(s.channel, pr), getSelf());
    }

    /**
     * 修改代码分区状态
     * @param m
     * @return
     */
    private BMessage chgPartStatus(BMessage m) {
        try {
            int part_id = m.p(BProps.PART_ID);
            PartitionStatus status = m.getEnumProperty(BProps.PART_STATUS, PartitionStatus.class);
            if(!parts.containsKey(part_id)){
                return BMessage.c()
                        .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
                        .p(BProps.RESULT_DESC, "part not found");
            }
            Mem x = parts.get(part_id);
            if(status == x.c.getStatus()){
                return BMessage.c()
                        .p(BProps.RESULT_CODE, RetCode.OK)
                        .p(BProps.RESULT_DESC, "status not changed");
            }
            x.c.setStatus(status);
            brk.tell(new BrkActor.PartChg(part_id, x.ref, status), getSelf());
            x.ref.tell(status, getSelf());

        }catch(NoSuchPropertyException ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }
        return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);

    }

    private BMessage startPart(BMessage m) {
        // props: TARGET_TOPIC  CLIENT_ID， PART_NUM, PART_ID, PART_STATUS

        try {
            String body = m.getContentAsString();
            System.out.println("===="+body);
            PartConfig c = JSONSerializable.fromJson(body, PartConfig.class);
            int part_id = c.getPartId();

            if (parts.containsKey(c.getPartId())) {
                return BMessage.c()
                        .p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                        .p(BProps.RESULT_DESC, "part_id " + c.getPartId() + " dulplicated");
            }

            ActorRef mem = getContext().actorOf(
                    Props.create(MemActor.class, c, store, brk, reply, getSelf()),
                    String.valueOf(part_id));

            Mem x = new Mem();
            x.c = c;
            x.ref = mem;
            parts.put(part_id, x);
            log.info("create part for:{}", mem.path().toString());

            return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
        }catch(NoSuchPropertyException ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }
    }

}
