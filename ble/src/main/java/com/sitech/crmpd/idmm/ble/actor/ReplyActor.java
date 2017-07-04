package com.sitech.crmpd.idmm.ble.actor;


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
import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

public class ReplyActor extends AbstractActor {
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(ReplyActor.class);


    private Router router;
    public static class Msg {
        public Channel channel;
        public FramePacket packet;
        public long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }

    private static class Worker extends AbstractActor{
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
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
        private void onReceive(Msg s) {
            try{
                if(s.packet != null)
                    s.channel.writeAndFlush(s.packet);
            }catch(Exception ex) {
                log.error("write reply to channel failed", ex);
            }
        }
    }

    public ReplyActor(int size) {
        List<Routee> routees = new ArrayList<Routee>();
        for (int i = 0; i < size; i++) {
            ActorRef r = getContext().actorOf(Props.create(Worker.class));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Msg.class, message -> {
                    router.route(message, getSender());
                })
                .match(Terminated.class, message -> {
                    router = router.removeRoutee(message.actor());
                    ActorRef r = getContext().actorOf(Props.create(Worker.class));
                    getContext().watch(r);
                    router = router.addRoutee(new ActorRefRoutee(r));
                })
                .build();
    }
}
