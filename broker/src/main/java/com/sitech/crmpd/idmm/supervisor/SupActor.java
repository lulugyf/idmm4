package com.sitech.crmpd.idmm.supervisor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群管理者 supervisor 的主事件处理actor
 */
public class SupActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public SupActor(int size) {

    }

    public static class Msg {
        public Channel channel;
        public FrameMessage frameMessage;
        public long create_time;
        public Msg(Channel c, FrameMessage p) {
            channel = c;frameMessage = p; create_time=System.currentTimeMillis();
        }
    }

    private void onReceive(Msg s) {
        try{
            if(s.frameMessage != null)
                s.channel.writeAndFlush(s.frameMessage);
        }catch(Exception ex) {
            log.error("write reply to channel failed", ex);
        }
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(Msg.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }
}
