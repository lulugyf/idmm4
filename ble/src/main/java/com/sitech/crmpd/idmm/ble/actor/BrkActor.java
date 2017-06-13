package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by gyf on 5/1/2017.
 */
public class BrkActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private HashMap<Integer, PartChg> parts = new LinkedHashMap<>();


    public static class Msg {
        public Channel channel;
        public FramePacket packet;
        public long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }
    // 分区状态通知
    public static class PartChg{
        public int part_id;
        public ActorRef ref;
        public PartStatus status;
        public PartChg(int part_id, ActorRef ref, PartStatus status) {
            this.part_id = part_id;
            this.ref = ref;
            this.status = status;
        }
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
                .match(PartChg.class, s-> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    private void onReceive(PartChg s){
        parts.put(s.part_id, s);
    }
    private void onReceive(Msg s) {
        FramePacket p = s.packet;
        int seq = p.getSeq();
        BMessage m = p.getMessage();

        switch(p.getType() ) {
            case HEARTBEAT:
                log.info("HEARTBEAT, {}", System.currentTimeMillis());
                break;
            case BRK_CONNECT:
                log.info("BRK_CONNECT, {}", System.currentTimeMillis());
                break;

            case BRK_SEND_COMMIT:
            case BRK_PULL:
            case BRK_COMMIT:
            case BRK_RETRY:
            case BRK_ROLLBACK:
            case BRK_SKIP:
            {
                if(!m.existProperty(BProps.PART_ID)){
                    log.error("request without part_id");
                    FramePacket f = new FramePacket(FrameType.CMD_PT_START_ACK, BMessage.c()
                            .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                            .p(BProps.RESULT_DESC, "request without part_id"), seq);
                    s.channel.writeAndFlush(f);
                    break;
                }
                int part_id = m.p(BProps.PART_ID);
//                ActorSelection ref = getContext().actorSelection(basePath+part_id);
                if(!parts.containsKey(part_id)) {
                    // part not found
                    log.error("part {} not found", part_id);
                    FramePacket f = new FramePacket(FrameType.CMD_PT_START_ACK, BMessage.c()
                            .p(BProps.RESULT_CODE, RetCode.BAD_REQUEST)
                            .p(BProps.RESULT_DESC, "part not found:"+part_id), seq);
                    s.channel.writeAndFlush(f);
                }else {
                    log.info("operator: {} to part {} seq {}", p.getType(), part_id, p.getSeq());
                    parts.get(part_id).ref.tell(new MemActor.Msg(s.channel, p), getSelf());
                }
            }
                break;
            default:
                log.error("unknown command {}", p.getType().code());
                break;
        }
    }
}
