package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by gyf on 5/1/2017.
 * 处理由broker发来的报文, 业务通讯的关键管道
 */
public class BrkActor extends AbstractActor {
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(BrkActor.class);
    private HashMap<Integer, PartState> parts = new LinkedHashMap<>();


    public static class Msg {
        public Channel channel;
        public FramePacket packet;
        public long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }
    // 分区状态通知
    public static class PartState{
        public int part_id;
        public PartStatus status;
        public ActorRef ref;
        public PartConfig pc;

        public PartState(ActorRef ref, PartConfig pc) {
            this.part_id = pc.getPartId();
            this.ref = ref;
            this.status = pc.getStatus();
            this.pc = pc;
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
                .match(PartState.class, s-> {
                    onReceive(s);
                })
                .match(Integer.class, s-> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    // 删除分区， 该分区已经离线
    private void onReceive(Integer partid){
        parts.remove(partid);
    }

    // 更新分区配置数据, 在通讯繁忙的情况下可能无法及时更新到
    private void onReceive(PartState s){
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
