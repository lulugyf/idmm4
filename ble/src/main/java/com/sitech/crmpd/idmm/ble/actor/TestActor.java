package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by gyf on 5/1/2017.
 * 处理由broker发来的报文, 业务通讯的关键管道
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TestActor extends AbstractActor {
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(TestActor.class);

    @Resource
    private MetricRegistry metrics;

    private Counter counter;
    private Meter meter;
    private String bb;

    public TestActor(String bb) {
        this.bb = bb;
    }

    @PostConstruct
    private void post() {
        counter = metrics.counter("testActor");
        meter = metrics.meter("testActor_meter");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s-> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    // 删除分区， 该分区已经离线
    private void onReceive(String s){
        meter.mark();
        System.out.println(bb+" from TestActor: "+s+" "+getSelf().path());
//        counter.dec();
    }
}
