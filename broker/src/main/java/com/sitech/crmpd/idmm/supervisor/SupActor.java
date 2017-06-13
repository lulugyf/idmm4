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
import com.alibaba.fastjson.JSON;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import com.sitech.crmpd.idmm.netapi.BMessage;
import com.sitech.crmpd.idmm.netapi.BProps;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import com.sitech.crmpd.idmm.netapi.FrameType;
import com.sitech.crmpd.idmm.supervisor.stru.BLEState;
import com.sitech.crmpd.idmm.supervisor.stru.QueueState;
import com.sitech.crmpd.idmm.util.BZK;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 集群管理者 supervisor 的主事件处理actor
 *
 * 需要保留的几个数据结构:
 * 1. BLE -> 分区
 * 2. 队列 -> 分区
 * 3. 请求id -> 请求包的数据
 * 分区的结构体使用PartConfig 就够了
 */
public class SupActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private int seq = 1; //请求id的累加数字
    private Map<Integer, FramePacket> requests = new HashMap<>(); // 保存请求id对应的请求包, 以便收到应答时做处理

    private Map<String, BLEState> bles = new HashMap<>(); // bleid -> ble状态
    private Map<String, QueueState> queues = new HashMap<>(); // qid -> 队列状态

    private Bootstrap bootstrap;
    private BZK zk;
    private AtomicInteger partid = new AtomicInteger(0); //分区id分配累加器

    public SupActor(Bootstrap bootstrap, BZK zk) {
        this.bootstrap = bootstrap;
        this.zk = zk;
        partid.set(zk.getMaxPartid());
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
                .match(String[].class, s->{
                    try{
                        onReceive(s);
                    } catch (Throwable throwable) {
                        log.error("deal with String[] failed", throwable);
                    }
                })
                .match(FramePacket.class, s->{
                    try{
                        onReceive(s);
                    } catch (Throwable throwable) {
                        log.error("deal with FramePacket failed", throwable);
                    }
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    /**
     * 状态变更通知
     * ble: (bleid),  有新的ble上线
     * blelist: (null), zk的watcher通知, ble列表变更
     * @param s
     */
    private void onReceive(String[] s) {
        if(s == null)
            return;
        if(s.length <= 2)
            return;
        final String tp = s[0];
        if("blelist".equals(tp)){
            Tools.bleListChg(bles, zk, bootstrap, getSelf());
        }else if("ble".equals(tp)){
            //done 发起查询
            String bleid = s[1];
            BLEState b = bles.get(bleid);
            FramePacket f = new FramePacket(FrameType.CMD_PT_QUERY,
                    BMessage.c().p(BProps.BLE_ID, bleid), seq++ );
            requests.put(f.getSeq(), f);
            b.ch.writeAndFlush(f);
        }
    }

    /**
     * 接收到ble的应答报文或者事件通知
     * @param fr 通讯报文
     */
    private void onReceive(FramePacket fr){
        switch(fr.getType()){
            case CMD_PT_QUERY_ACK: {
                // 查询应答
                FramePacket f = requests.remove(fr.getSeq());
                String bleid = f.getMessage().p(BProps.BLE_ID);
                BLEState b = bles.get(bleid);
                String body = fr.getMessage().getContentAsString();
                log.debug("query reply from BLE: {}", body);

                List<PartConfig> l = JSON.parseArray(body, PartConfig.class);
                for (PartConfig p : l) {
                    final String qid = p.getQid();
                    QueueState q = queues.getOrDefault(qid, new QueueState(qid));
                    queues.put(qid, q);

                    q.qryAdd(p);

                    b.parts.add(p);
                }
                b.query_stat = System.currentTimeMillis(); // 已执行过查询， 更新状态

                checkQueryDone();

            }
                break;

        }

    }

    /**
     * 检查是否全部BLE都查询过了
     */
    private void checkQueryDone() {
        // TODO check if all query done, then start check parts
        for(BLEState b: bles.values()){
            if(b.query_stat == 0)
                return;
        }

        log.info("begin check parts");

    }
}
