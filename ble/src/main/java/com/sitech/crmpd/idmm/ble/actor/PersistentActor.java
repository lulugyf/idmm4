package com.sitech.crmpd.idmm.ble.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PersistentActor extends AbstractActor {
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(PersistentActor.class);

    private Router router;
    public static class StoreMsg {
        public Channel channel;
        public FramePacket packet;
        public long create_time;
        public StoreMsg(Channel c, FramePacket p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }

    private static class Worker extends AbstractActor{
//        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
//        private static final Logger log = LoggerFactory.getLogger(PersistentActor.class);
        public Receive createReceive(){
            return receiveBuilder()
                    .match(Oper.class, s -> {
                        try {
                            onReceive(s);
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    })
                    .matchAny(o -> log.info("received unknown message:{}", o))
                    .build();
        }
        private void onReceive(Oper s) {
            log.info("got {} seq {}", s.type, s.seq);
            switch(s.type){
                case addOP: {
                    Oper o = new Oper(Oper.OType.addOP1);
                    o.channel = s.channel;
                    o.mi = s.mi;
                    o.seq = s.seq;
                    getSender().tell(o, getSelf());
                    log.info("send addOP1 to memactor, {}, seq {}", s.mi.getMsgid(), s.seq);
                }
                    break;
                case getOP:
                    // nothing to to for getop
                    break;
                case failOP:
                    // store and no next message
                    break;
                case ackOP:
                    Oper o = new Oper(Oper.OType.ackOP1);
                    o.channel = s.channel;
                    o.msgid = s.msgid;
                    o.seq = s.seq;
                    o.process_time = s.process_time;
                    o.next = s.next;
                    getSender().tell(o, getSelf());
                    log.info("send ackOP1 to memactor, {}", s.msgid);
                    break;
                case skipOP:
                    s.channel.writeAndFlush(new FramePacket(FrameType.BRK_SKIP_ACK,
                            BMessage.c().p(BProps.RESULT_CODE, RetCode.OK), s.seq ) );
                    break;
                default:
                    log.error("unknow oper type: {}", s.type);
            }
//            try{
//                s.channel.writeAndFlush(s.packet);
//            }catch(Exception ex) {
//                log.error("write reply to channel failed", ex);
//            }
        }
    }

    public PersistentActor(int size) {
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
                .match(Oper.class, message -> {
                    router.route(message, getSender());
                })
                .match(StoreMsg.class, s -> {
                    router.route(s, getSender());
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
