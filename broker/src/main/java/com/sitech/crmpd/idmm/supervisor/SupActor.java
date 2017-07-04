package com.sitech.crmpd.idmm.supervisor;


import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.alibaba.fastjson.JSON;
import com.sitech.crmpd.idmm.broker.config.Loader;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.cfg.QueueConfig;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.netapi.*;
import com.sitech.crmpd.idmm.supervisor.stru.BLEState;
import com.sitech.crmpd.idmm.supervisor.stru.QueueState;
import com.sitech.crmpd.idmm.util.ZK;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(SupActor.class);

    private int seq = 1; //请求id的累加数字
    private Map<Integer, FramePacket> requests = new HashMap<>(); // 保存请求id对应的请求包, 以便收到应答时做处理

    private Map<String, BLEState> bles = new HashMap<>(); // bleid -> ble状态
    private Map<String, QueueState> queues = new HashMap<>(); // qid -> 队列状态

    private Bootstrap bootstrap;
    private ZK zk;
    private AtomicInteger partid = new AtomicInteger(0); //分区id分配累加器

    private Loader conf; //配置数据加载器

    public SupActor(Bootstrap bootstrap, ZK zk, Loader conf) {
        this.bootstrap = bootstrap;
        this.zk = zk;
        partid.set(zk.getMaxPartid());
        this.conf = conf;
    }

    public static class Msg {
        public Channel channel;
        public FramePacket f;
        public long create_time;
        public Msg(Channel c, FramePacket p) {
            channel = c; f = p; create_time=System.currentTimeMillis();
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
                .match(String[].class, s->{
                    try{
                        onReceive(s);
                    } catch (Throwable throwable) {
                        log.error("deal with String[] failed", throwable);
                    }
                })
                .match(Channel.class, s->{
                    try{
                        onReceive(s);
                    } catch (Throwable throwable) {
                        log.error("deal with String[] failed", throwable);
                    }
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    /**
     * ble 状态连接断开 触发后续处理：
     * 1. 删除zk上该ble节点
     * 2. 先更新 zk 上该BLE上的分区状态为shut
     * 3. 为每个分区分别启动新的 join 和 leave状态的分区
     */
    private void onReceive(Channel s) {
        BLEState ble = null;
        for(BLEState b: bles.values()){
            if(b.ch == s){
                bles.remove(b.id);
                log.warn("BLE {} offline", b.id);
                ble = b;

                break;
            }
        }
        if(ble == null)
            return;

        // DONE trigger a ble leave process

        zk.removeBLE(ble.id); //删除zk上的ble节点

        // 对于离线的BLE， 找出其上的全部分区，从zk删除
        if(bles.size() == 0){
            log.warn("BLE shut: no ble online, skip");
            return; // 没有在线BLE， 后边的事情也不要处理了
        }
        log.debug("BLE shut: setzk, size={}", ble.parts.size());
        for(PartConfig pc: ble.parts){
            pc.setRunStatus(PartConfig.RUN_STATUS_INIT);
            pc.setStatus(PartStatus.SHUT);
            zk.setPart(pc);
        }
        zk.partChanged();

        List<QueueState.PartOP> ol = new LinkedList<>();
        for(PartConfig pc: ble.parts){
            QueueState qs = queues.get(pc.getQid());
            if(qs == null){
                log.error("BLE down: can not found qid: {}", pc.getQid());
                continue;
            }
            qs.partShut(pc, partid, ol);
        }
        log.debug("BLE shut: start recovery parts, size={}", ol.size());
        if(ol.size() > 0) {
            if(bles.size() > 0)
                sendPartRequest(ol);
            else
                log.warn("not ble online");
        }
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
        if(s.length < 2)
            return;
        final String tp = s[0];
//        log.warn("receive cmd message: {}", tp);
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
        }else if("Tick".equals(tp)) {
            tick();
        }
    }


    /**
     * 接收到ble的应答报文或者事件通知
     * @param s 通讯报文
     */
    private void onReceive(Msg s) {
        FramePacket fr = s.f;
        FrameType ftype = fr.getType();
        FramePacket f = null;
        BMessage bm = null;

        if(ftype == FrameType.HEARTBEAT_ACK)
            return;

        if((ftype.code() & 0x80 ) == 0x80){ //响应报文
            f = requests.remove(fr.getSeq());
            bm = f.getMessage();
        }
        BMessage bmr = fr.getMessage();
        log.debug("receive Msg, type: {}",
                fr.getType());

        switch(fr.getType()){
            case CMD_PT_QUERY_ACK:
            {
                // 查询应答
                RetCode rcode = fr.getMessage().getEnumProperty(BProps.RESULT_CODE, RetCode.class);
                String bleid = bm.p(BProps.BLE_ID);
                if(rcode != RetCode.OK){
                    log.error("ble {} query  failed: {} {}", bleid, rcode, bmr.p(BProps.RESULT_DESC));
                    break;
                }
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
                b.stat = 1; // 已执行过查询， 更新状态
            }
                break;
            case CMD_PT_START_ACK:
            case CMD_PT_CHANGE_ACK:
            {
                //分区启动应答
                String qid = bm.p(BProps.QID);
                int partid = bm.p(BProps.PART_ID);
                RetCode rcode = fr.getMessage().getEnumProperty(BProps.RESULT_CODE, RetCode.class);
                if(rcode != RetCode.OK){
                    log.error("part {} {} start failed: {} {}",
                            qid, partid, rcode, bmr.p(BProps.RESULT_DESC));
                    break;
                }

                QueueState qs = queues.get(qid);
                if(qs == null){
                    log.error("queue {} not found", qid);
                }else{
                    List<QueueState.PartOP> rl = qs.partStarted(partid);
                    if(rl != null && rl.size() > 0)
                        sendPartRequest(rl);

                    // 更新zk分区数据
                    PartConfig pc = qs.getPart(partid);
                    zk.setPart(pc);

                    // 如果 该队列全部分区启动完成, 触发broker更新分区数据
                    if(qs.isAllPartStarted()){
                        zk.partChanged();
                    }
                }
            }
                break;
            case CMD_PT_LEAVE_DONE:
            {
                //leave 分区消费完成
                String qid = bmr.p(BProps.QID);
                int partid = bmr.p(BProps.PART_ID);
                QueueState qs = queues.get(qid);
                BMessage rm = BMessage.c();
                if(qs == null){
                    log.error("CMD_PT_LEAVE_DONE queue {} not found", qid);
                    rm.p(BProps.RESULT_CODE, RetCode.INTERNAL_DATA_ACCESS_EXCEPTION)
                            .p(BProps.RESULT_DESC, "queue "+qid +" not found on supervisor");
                }else {
                    PartConfig pc = qs.getPart(partid);
                    List<QueueState.PartOP> rl = qs.leaveDone(partid);
                    if (rl != null && rl.size() > 0)
                        sendPartRequest(rl);

                    zk.delPart(pc); //删除zk上的分区数据
                    BLEState ble = bles.get(pc.getBleid());
                    ble.parts.remove(pc);

                    // 回应答
                    rm.p(BProps.RESULT_CODE, RetCode.OK);
                }
                FramePacket f1 = new FramePacket(FrameType.CMD_PT_LEAVE_DONE_ACK,
                        rm, fr.getSeq());
                s.channel.writeAndFlush(f1);
                log.info("LEAVE DONE for partid {}", partid);
            }
                break;
            case HEARTBEAT_ACK:
                break;
            default:
                log.error("unkown message type {}", fr.getType());
        }

    }

    /**
     * 检查是否全部BLE都查询过 并返回了结果
     */
    private boolean checkQueryDone() {
        for(BLEState b: bles.values()){
            if(b.stat  != 1)
                return false;
        }
        return true;
    }

    /**
     * 在没有BLE在线的情况下， 清除所有的队列分区数据, 并删除zk上的分区数据
     */
    private void clear() {
        if(queues.size() > 0)
            queues.clear();
        zk.clearParts();
    }

    /**
     * 定时触发任务
     */
    private void tick() {
        if(bles.size() == 0){
            log.warn("there are no ble online");
            clear();
            return;
        }
        if(!checkQueryDone()){
            log.warn("bles query not complete, tick task ignored");
            return;
        }
        // 0. send heartbeat, and check if online
        for(BLEState b: bles.values()){
//            log.info("write a heartbeat to {}", b.ch.remoteAddress());
            b.ch.writeAndFlush(new FramePacket(FrameType.HEARTBEAT,
                    BMessage.c(), seq++));
        }

        //1. 核对zk与查询ble得到的分区数据
        for(QueueState qs: queues.values()){
            qs.compareZK(zk);
        }

        //2. 读取配置， 然后核对启动分区
        int pid_last = partid.get();
        List<QueueConfig> ql = conf.loadQueues();
        for(QueueConfig qc: ql){
            String qid = qc.getQid();
            QueueState qs = queues.getOrDefault(qid, new QueueState(qid));
            queues.put(qid, qs);
            if(qs.size() == 0){
                log.warn("qid {} not found", qid);
            }

            List<QueueState.PartOP> rl = qs.checkParts(partid, qc);
            if(rl.size() > 0)
                sendPartRequest(rl);
        }

        //3. 检查更新分区id种子到zk
        int pid_now = partid.get();
        if(pid_now != pid_last)
            zk.setMaxPartid(pid_now);
    }

    /**
     * 向BLE发送分区变更或启动请求
     *
     * 1. 先发送新增的（包括ready join)
     *
     * 有关联的需要等到关联分区启动完成后再修改状态
     * @param rl
     */
    private void sendPartRequest(List<QueueState.PartOP> rl) {
        log.debug("sendPartRequest size {}", rl.size());
        // 1. 先发送新增的（包括ready join)
        List<BLEState> bl = new ArrayList<>(bles.size());
        bl.addAll(bles.values());
        Collections.sort(bl);

        int bidx = 0;
        for(QueueState.PartOP po: rl){
            PartConfig pc = po.part;
            if(po.optype == QueueState.PartOP.START){
                //分配新的分区
                if(bidx >= bl.size())
                    bidx = 0;
                BLEState b = bl.get(bidx);
                if(pc.getStatus() != PartStatus.LEAVE)
                    bidx ++;
                pc.setBleid(b.id);
                b.parts.add(pc);

                int s = seq++;
                FramePacket f = new FramePacket(FrameType.CMD_PT_START,
                        BMessage.create(JSON.toJSONString(pc))
                                .p(BProps.PART_ID, pc.getPartId())
                        .p(BProps.QID, pc.getQid()), s );
                requests.put(s, f);
                b.ch.writeAndFlush(f);

                log.info("start part qid={} num={} id={} status={}",
                        pc.getQid(), pc.getPartNum(), pc.getPartId(), pc.getStatus());

            }else if(po.optype == QueueState.PartOP.MODIFY){
                //修改分区状态
                int s = seq++;
                FramePacket f = new FramePacket(FrameType.CMD_PT_CHANGE,
                        BMessage.create(JSON.toJSONString(pc))
                                .p(BProps.PART_ID, pc.getPartId())
                                .p(BProps.QID, pc.getQid()), s );
                requests.put(s, f);
                BLEState b = bles.get(pc.getBleid());
                b.ch.writeAndFlush(f);

                log.info("change part {} status to {}",
                        pc.getPartId(),  pc.getStatus());
            }
        }
    }

}
