package com.sitech.crmpd.idmm.broker.actor;


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
import com.sitech.crmpd.idmm.client.api.*;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class PersistActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    private ActorRef reply;
    private Router router;
    public static class Msg {
        public Channel channel;
        public FrameMessage packet;
        public long create_time;
        public Msg(Channel c, FrameMessage p) {
            channel = c;packet = p; create_time=System.currentTimeMillis();
        }
    }

    private static class Worker extends AbstractActor{
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
        private ActorRef reply;
        Worker(ActorRef r) {this.reply = r;}
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
            FrameMessage frameMessage = s.packet;
            Message message = frameMessage.getMessage();
            Message answer = null;
            MessageType answerType = null;
            switch(frameMessage.getType()) {
                case SEND:
                    //TODO save to persistent
                    answer = Message.create();
                    answer.setProperty(PropertyOption.MESSAGE_ID, message.getId());
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.OK);
                    answerType = MessageType.ANSWER;
                    break;
                case ANSWER:
                    // pull 返回了消息, 需要取回原本的消息, 返回的属性字段有:
                {
                    String msgid = message.getId();
                    answerType = MessageType.ANSWER;
                    answer = message;
                }
                    break;
                default:
                    break;
            }
            if(answer != null) {
                FrameMessage fr = new FrameMessage(answerType, answer);
                reply.tell(new ReplyActor.Msg(s.channel, fr), getSelf());
            }
        }
    }


    public PersistActor(int size, ActorRef reply) {
        List<Routee> routees = new ArrayList<Routee>();
        for (int i = 0; i < size; i++) {
            ActorRef r = getContext().actorOf(Props.create(Worker.class, reply));
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        this.reply = reply;
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
                    ActorRef r = getContext().actorOf(Props.create(Worker.class, reply));
                    getContext().watch(r);
                    router = router.addRoutee(new ActorRefRoutee(r));
                })
                .build();
    }
}
