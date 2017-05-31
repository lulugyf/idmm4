package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.sitech.crmpd.idmm.broker.config.ConsumeParts;
import com.sitech.crmpd.idmm.broker.config.Parts;
import com.sitech.crmpd.idmm.broker.handler.LogicHandler;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import com.sitech.crmpd.idmm.util.BZK;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gyf on 5/1/2017.
 */
public class GetPartsFromZKActor extends AbstractActor {
    private static final Logger log = LoggerFactory.getLogger(GetPartsFromZKActor.class);

    private BZK zk;
    private ActorRef ble;
    private LogicHandler logicHandler;
    private AtomicInteger partsChanged = new AtomicInteger(0);


    public GetPartsFromZKActor(BZK zk, ActorRef ble, LogicHandler logicHandler) {
        this.zk = zk;
        this.ble = ble;
        this.logicHandler = logicHandler;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .match(Integer.class, i -> {
                    onReceive(i);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }


    private void onReceive(String s) {
        int times = partsChanged.getAndSet(0); //获得被触发的次数
        if( times == 0){
            return;
        }
        Parts parts = new Parts();
        ConsumeParts cp = new ConsumeParts();
        parts.setAllParts(zk, cp);
        logicHandler.setParts(parts);
        ble.tell(cp, ActorRef.noSender());
        log.warn("part status changed, times: {}", times);

    }
    private void onReceive(Integer i) {
        if(partsChanged.addAndGet(1) == 1){
            // 延迟触发, 5秒后向自己发送消息, 这期间的其它同类通知一次处理
            ActorSystem system = getContext().getSystem();
            system.scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS),
                    getSelf(), "foo", system.dispatcher(), null);
        }
    }

}
