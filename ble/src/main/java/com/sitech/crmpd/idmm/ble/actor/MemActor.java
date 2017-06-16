package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.ble.mem.MsgIndex;
import com.sitech.crmpd.idmm.ble.mem.JournalOP;
import com.sitech.crmpd.idmm.ble.mem.MemQueue;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.netapi.*;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import io.netty.channel.Channel;

/**
 * Created by gyf on 5/1/2017.
 */
public class MemActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private String qid;
    private int maxOnWay;
    private int part_num ;
    private int part_id;
    private PartStatus status;
    private PartConfig c;
    private MemQueue mq;

    private ActorRef persistent;
    private ActorRef brk;
    private ActorRef reply;
    private ActorRef cmd;

    private int default_priority = 200; //TODO 默认优先级从配置中获取

    public MemActor(PartConfig c,
                    ActorRef persistent, ActorRef brk,  ActorRef reply, ActorRef cmd,
                    int seq) {
        this.c = c;
        this.persistent = persistent;
        this.brk = brk;
        this.reply = reply;
        this.cmd = cmd;

        this.qid = c.getQid();
        this.part_num = c.getPartNum();
        this.part_id = c.getPartId();
        this.status = c.getStatus();
        maxOnWay = c.getMaxRequest();
        mq = new MemQueue("", qid, maxOnWay);
        if(status == PartStatus.LEAVE) {
            // TODO loading index data from store
            new Thread(){
                public void run() {
                    _startFinished(seq);
                }
            }.start();

        }else {
            _startFinished(seq);
        }
    }

    private void _startFinished(int seq) {
        // 通知brkActor 有新的part上线了
        brk.tell(new BrkActor.PartState(getSelf(), c), getSelf());

        // 然后告知 cmdActor 启动完成, 以便应答Supervisor
        cmd.tell(new CmdActor.Notify(seq, "ok"), ActorRef.noSender());
    }

    public static class Msg
    {
        public Channel channel;
        public FramePacket packet;
        public long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }
    public static class PartChg{
        int seq;
        PartConfig pc;
        public PartChg(int seq, PartConfig pc) {
            this.seq = seq;
            this.pc = pc;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Msg.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable ex) {
                        log.error("receive Msg failed", ex);
                    }
                })
                .match(Oper.class, o -> {
                    try{
                        onReceive(o);
                    }catch(Throwable ex){
                        log.error("receive oper failed", ex);
                    }
                })
                .match(PartChg.class, s ->{
                    log.warning("part {} status changed, from {} to {}", part_id, status, s);
                    partChg(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    private void partChg(PartChg pc) {
        this.c = pc.pc;
        MemQueue.Conf conf = new MemQueue.Conf();
        conf.maxOnway = c.getMaxRequest();
        conf.minTimeoutMs = c.getMinTimeout()*1000;
        conf.maxTimeoutMs = c.getMaxTimeout()*1000;
        conf.warnMsg = c.getWarnMessages();
        conf.maxMsg = c.getMaxMessages();

        this.status = c.getStatus();

        mq.setConf(conf);

        cmd.tell(new CmdActor.Notify(pc.seq, "ok"), ActorRef.noSender());

        if(status == PartStatus.LEAVE && mq.size() == 0){
            // 发起leavedone通知
            cmd.tell(new CmdActor.LeaveDone(qid, part_id), getSelf());
        }
    }

    private void onReceive(Oper s) {
        FramePacket fr = null;
        switch (s.type) {
            case addOP1:
                // after persistent
                mq.add(s.mi);
                log.info("add index {}, size: {} seq {}", s.mi.getMsgid(), mq.size(), s.seq);
                fr = new FramePacket(FrameType.BRK_SEND_COMMIT_ACK,
                        BMessage.c().p(BProps.RESULT_CODE, RetCode.OK), s.seq ) ;
                break;
            case ackOP1: {
                // commit
                boolean r = mq.ack(s.msgid);
                if(!r){
                    log.error("ack to mem failed");
                    fr = new FramePacket(FrameType.BRK_COMMIT_ACK,
                            BMessage.c().p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                                .p(BProps.RESULT_DESC, "ack to mem failed"), s.seq ) ;
                }else{
                    log.info("commit success {}", s.msgid);
                    fr = new FramePacket(FrameType.BRK_COMMIT_ACK,
                            BMessage.c().p(BProps.RESULT_CODE, RetCode.OK), s.seq );

                    if(status == PartStatus.LEAVE && mq.size() == 0){ // leave分区消费完成
                        cmd.tell(new CmdActor.LeaveDone(qid, part_id), getSelf());
                    }
                }
            }
                break;
            default:
                log.error("invalid OPType {}", s.type);
                break;
        }
        if(fr != null) {
            reply.tell(new ReplyActor.Msg(s.channel, fr), getSelf());
        }
    }

    private boolean _checkStatus(FrameType tp, Channel ch, int seq) {
        boolean refuse = false;
        String desc = null;
        switch(tp){
            case BRK_SEND_COMMIT:
                refuse = status == PartStatus.LEAVE;
                desc = "leaving not allow send";
                break;
            case BRK_COMMIT:
            case BRK_PULL:
            case BRK_RETRY:
            case BRK_SKIP:
                refuse = status == PartStatus.JOIN;
                desc = "join now allow consume";
                break;
        }
        if(refuse){
            ch.writeAndFlush(new FramePacket(FrameType.BRK_RETRY_ACK,
                    BMessage.c().p(BProps.RESULT_CODE, RetCode.REQUEST_REFUSE)
                            .p(BProps.RESULT_DESC, "leaving not allow send"), seq ) );
        }
        return refuse;
    }

    private void onReceive(Msg s) {
        FramePacket p = s.packet;
        int seq = p.getSeq();
        BMessage m = p.getMessage();
        BMessage mr = null;

        if(_checkStatus(p.getType(), s.channel, seq))
            return;
        try {
            switch (p.getType()) {
                case BRK_SEND_COMMIT:
                    mr = sendCommit(m, s.channel, seq);
                    if(mr == null)
                        return; //to store actor
                    break;
                case BRK_PULL:
                    mr = pull(m, s.channel, seq);
                    break;
                case BRK_COMMIT:
                {
                    log.info("BRK_COMMIT, {}", System.currentTimeMillis());
                    String msgid = m.p(BProps.MESSAGE_ID);
                    if(mq.exists(msgid)) {
                        // persistent first
                        Oper o = new Oper(Oper.OType.ackOP);
                        o.msgid = msgid;
                        o.channel = s.channel;
                        o.seq = p.getSeq();
                        persistent.tell(o, getSelf());
                        return;
                    }else{
                        // already committed
                        log.info("commit: messageid {} not exists, return ok", msgid);
                        mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
                    }
                }
                    break;
                case BRK_RETRY:
                    // 更新锁定时间, 不操作store
                    log.info("BRK_RETRY, {}", System.currentTimeMillis());
                {
                    int after = m.p(BProps.RETRY_AFTER);
                    String msgid = m.p(BProps.MESSAGE_ID);
                    if(mq.retry(msgid, after)){
                        mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
                    }else{
                        log.info("retry msg {} failed", msgid);
                        mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                                .p(BProps.RESULT_DESC, "retry msg failed");
                    }
                }
                    break;
                case BRK_SKIP:
                    log.info("BRK_SKIP, {}", System.currentTimeMillis());
                {
                    String msgid = m.p(BProps.MESSAGE_ID);
                    String desc = m.p(BProps.RESULT_DESC);
                    String rcode = m.getEnumProperty(BProps.RESULT_CODE, RetCode.class).name();
                    MsgIndex mi = mq.skip(msgid, rcode, desc);
                    if(mi != null) {
                        Oper o = new Oper(Oper.OType.skipOP);
                        o.mi = mi;
                        o.seq = p.getSeq();
                        o.channel = s.channel;
                        persistent.tell(o, getSelf());
                        return;
                    }else{
                        log.error("SKIP {} failed", msgid);
                        mr = BMessage.c().p(BProps.RESULT_CODE, RetCode.INTERNAL_SERVER_ERROR)
                                        .p(BProps.RESULT_DESC, "skip msg failed");
                    }
                }
                    break;
                case BRK_ROLLBACK:
                {
                    String msgid = m.p(BProps.MESSAGE_ID);
                    boolean r = mq.rollback(msgid);
                    mr = BMessage.c().p(BProps.RESULT_CODE, r?RetCode.OK:RetCode.INTERNAL_SERVER_ERROR);
                    if(!r)
                        mr.p(BProps.RESULT_DESC, "rollback message failed");
                }
                    break;

                default:
                    log.error("unknown command {}", p.getType().code());
                    break;
            }
        }catch(NoSuchPropertyException e){
            mr = BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, e.toString());
        }

        if(mr == null){
            mr = BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
                    .p(BProps.RESULT_DESC, "invalid request");
        }

        FramePacket pr = new FramePacket(
                FrameType.valueOfCode(p.getType().code()|0x80),
                mr, seq);
        reply.tell( new ReplyActor.Msg(s.channel, pr), getSelf());
    }

    private BMessage sendCommit(BMessage m, Channel ch, int seq) {
        MsgIndex mi = new MsgIndex();
        mi.setMsgid(m.p(BProps.MESSAGE_ID));
        mi.setGroupid(m.p(BProps.GROUP));
        int priority = default_priority;
        if(m.existProperty(BProps.PRIORITY))
            priority = m.p(BProps.PRIORITY);
        mi.setPriority(default_priority);
        log.info("BRK_SEND_COMMIT, {}", System.currentTimeMillis());
        if(!mq.exists(mi.getMsgid())) {
            Oper o = new Oper(Oper.OType.addOP);
            o.mi = mi;
            o.channel = ch;
            o.seq = seq;
            persistent.tell(o, getSelf());
            log.info("send addOP to persistent seq {}", seq);
        }else{
            // duplicated with mem, return OK
            log.info("duplicated index");
            return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);
        }
        return null;
    }

    private BMessage pull(BMessage m, Channel ch, int seq) {
        int ptime = m.p(BProps.PROCESSING_TIME);
        log.info("BRK_PULL, ptime: {}", ptime);
        while (true) {
            JournalOP op = mq.get("", ptime);
            if(op == null){
                log.info("no message found");
                return BMessage.c().p(BProps.RESULT_CODE, RetCode.NO_MORE_MESSAGE);
            }
            if(op.op == JournalOP.OP.FAIL){
                log.info("save one failed index");
                Oper o = new Oper(Oper.OType.failOP);
                o.mi = op.mi;
                persistent.tell(o, getSelf());
            }else if(op.op == JournalOP.OP.GET){
                log.info("got one message from mem");
                MsgIndex mi = op.mi;
                return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK)
                                .p(BProps.MESSAGE_ID, mi.getMsgid())
                                .p(BProps.CONSUMER_RETRY, mi.getRetry())
                        .p(BProps.QSTATE, new int[]{mq.size(), mq.maxPriority(), mq.onwayLeft()});
                //TODO return message size to broker
            }else{
                log.error("invalid optype {}", op.op);
                break;
            }
        }
        return null;
    }
}

