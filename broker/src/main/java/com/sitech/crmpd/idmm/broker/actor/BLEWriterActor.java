package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gyf on 5/1/2017.
 */
public class BLEWriterActor extends AbstractActor {
    private static final Logger log = LoggerFactory.getLogger(BLEWriterActor.class);
    private ActorRef reply;

    private Channel ch;

    public BLEWriterActor(Channel ch, ActorRef reply) {
        this.ch = ch;
        this.reply = reply;
    }

    // 向BLE发送的请求包
    public static class Msg {
        Channel channel;
        FramePacket fm;
        long create_time;

        public Msg(Channel c, FramePacket f) {
            channel = c;fm = f; create_time=System.currentTimeMillis();
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
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }


    private void onReceive(Msg s) {
        try{
            ch.writeAndFlush(s.fm);
        }catch(Exception ex) {
            log.error("write failed", ex);
            // TODO 写失败后需要立刻向客户端应答异常
        }

    }

}
