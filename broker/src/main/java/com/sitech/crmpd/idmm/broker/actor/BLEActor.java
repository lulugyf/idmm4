package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by gyf on 5/1/2017.
 */
public class BLEActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ActorRef reply;
    private ActorRef store;
    private ActorRef brk;
    private HashMap<Integer, Mem> parts = new HashMap<>();
    private LinkedList<Mem> list = new LinkedList<>();

    // 保存最后发送消息给本BLE的supervisor的channel, 以便进行状态通知(leaving的分区消费完毕)
    private Channel supervisor;

    public static class Msg {
        Channel channel;
        FrameMessage fm;
        long create_time;
        public Msg(Channel c, FrameMessage f) {
            channel = c;fm = f; create_time=System.currentTimeMillis();
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
        }
    }

    private void onReceive(Msg s) {
        supervisor = s.channel;


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
            x.ref.tell(status, getSelf());

        }catch(NoSuchPropertyException ex){
            return BMessage.c()
                    .p(BProps.RESULT_CODE, RetCode.REQUIRED_PARAMETER_MISSING)
                    .p(BProps.RESULT_DESC, ""+ex.toString());
        }
        return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);

    }

    private BMessage startPart(BMessage m) {

            return BMessage.c().p(BProps.RESULT_CODE, RetCode.OK);

    }

}
