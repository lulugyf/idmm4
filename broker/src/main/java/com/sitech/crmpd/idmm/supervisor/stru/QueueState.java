package com.sitech.crmpd.idmm.supervisor.stru;

import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.cfg.QueueConfig;
import com.sitech.crmpd.idmm.util.ZK;
import com.sitech.crmpd.idmm.util.ch.ConsistentHash;
import com.sitech.crmpd.idmm.util.ch.StrHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by guanyf on 6/12/2017.
 * 单个队列（target_topic_id~concumer_client_id) 的状态
 */
public class QueueState {
    private static final Logger log = LoggerFactory.getLogger(QueueState.class);

    public String qid;
    public int num; //设定的分区数量
    public int maxOnway; //最大在途数， 只能设定在每个分区上， 分区间无法协调
    private Map<Integer, PartConfig> parts = new HashMap<>();

    // 一致性哈希环， leave状态的节点将不在其中， 但会在 parts 中
    private ConsistentHash<PartConfig> ch = new ConsistentHash<>(new StrHashFunction(),
            1, null);

    public QueueState(String qid) {
        this.qid = qid;
    }

    public int size() {
        return parts.size();
    }

    /**
     * 从BLE查询的结果更新到结构中, 构建一致性hash
     * @param pc
     */
    public void qryAdd(PartConfig pc) {
        pc.setRunStatus(PartConfig.RUN_STATUS_STARTED);
        if(pc.getPartNum() > num)
            num = pc.getPartNum();

        parts.put(pc.getPartId(), pc);
        switch(pc.getStatus()){
            case READY:
            case JOIN:
                ch.add(pc);
                break;
        }
    }

    /**
     * 队列状态：
     * 1 - 全部分区在线且状态都为ready
     * 0 - 全部分区都不在线
     */
    public int status;

    /**
     * 比较zk上的分区数据与保存的记录是否一致
     * 如果不一致， 则以保存记录为准 修改zk的
     * 需要比较下面的属性：
     * private int partNum;
     private PartStatus status;
     private String[] relatedPart; // leave 状态分区的关联 join 分区
     private String bleid;

     * @param zk zookeeper连接工具
     */
    public void compareZK(ZK zk){
//        log.info("comparing parts between zk and ble qid:{}", qid);
        List<PartConfig> zpts = zk.getParts(qid);
        Map<Integer, PartConfig> mz = new HashMap<>();
        for(PartConfig pc: zpts)
            mz.put(pc.getPartId(), pc);
        Map<Integer, PartConfig> ml = parts;

        boolean changed = false;

        //1 检查zk缺失， 补zk
        for(Integer pid: ml.keySet()){
            if(!mz.containsKey(pid)){
                changed = true;
                zk.setPart(ml.get(pid));
                log.warn("compareZK add to zk: qid: {} partid: {}", qid, pid);
            }
        }

        //2 检查记录缺失， 删zk
//        List<Integer> delPart = new LinkedList<>();
        for(Integer pid: mz.keySet()){
            if(!ml.containsKey(pid)){
                changed = true;
                zk.delPart(mz.get(pid));
//                mz.remove(pid);
//                delPart.add(pid);
                log.warn("compareZK remove from zk: qid: {}, partid: {}", qid, pid);
            }
        }
//        for(Integer pid: delPart) mz.remove(pid);

        //3 检查分区数据不一致
        for(PartConfig pl: ml.values()){
            PartConfig pz = mz.get(pl.getPartId());
            if(pz == null){
                log.error("partid {} not on zk", pl.getPartId());
                continue;
            }
            if(pl.getPartNum() != pz.getPartNum() ||
                    pl.getStatus() != pz.getStatus() ||
                    !pl.getBleid().equals(pz.getBleid())){
                changed = true;
                zk.setPart(pl);

                log.warn("compareZK zk ble not match, correct zk, qid: {} partid: {}",
                        qid, pl.getPartId());
            }
        }

        if(changed) {
            zk.partChanged();
            log.info("partChanged");

        }
    }

    /**
     * 检查是否全部分区为ready状态
     * @return
     */
    public boolean isReady() {
        for(PartConfig pc: parts.values()){
            if(pc.getStatus() != PartStatus.READY || pc.getRunStatus() != PartConfig.RUN_STATUS_STARTED)
                return false;
        }
        return true;
    }

    /**
     * 检查分区完整性
     * 检查规则:
     *     0. 基于已经存在的分区结构， 与配置的预期进行检查
     *     1. 每个分区num, 必须存在有对应ready或 join 状态分区, 否则不完整, 添加
     *     2. 每个join状态的分区, 必须有对应leave状态分区; 改为ready状态
     *     3. 多出的分区, 需进行分区的减少操作
     *     4. 重复num的分区, 回头想下该怎么搞 TODO duplicated part number? how to deal with
     *
     * 要新启动的分区， bleid 由后续的分配动作设定
     *
     * TODO forbidden mid-state queue to add/remove parts
     * TODO ble crushed while in mid-state, how to repair
     *
     * @param partid 分配分区id的种子, 函数返回后调用者需要检查数字是否更新过， 更新过则要把值保存到zk上
     * @param qc 配置结构体
     *
     * @return 返回需要处理的分区列表, 调用者拿到这个列表向BLE发送指令
     */
    public List<PartOP> checkParts(AtomicInteger partid, QueueConfig qc) {
        int pnum = qc.getPartCount();
        List<PartOP> ret = new LinkedList<>();

        if(!isReady()){
            log.error("queue of {} is in mid-status", qid);
            return ret;
        }

        for(int i=1; i<=pnum; i++){
            boolean found = false;
            for(PartConfig pc: parts.values()){
                if(pc.getPartNum() == i){
                    if(pc.getStatus() == PartStatus.READY){
                        found = true;
                        break;
                    }else if(pc.getStatus() == PartStatus.JOIN){
                        // join 状态的就要寻找是否有对应的leave分区
                        boolean existsLeave = false;
                        for(PartConfig p1: parts.values()){
                            if(p1.existsRelPart(pc.getPartId())){
                                existsLeave = true;
                                break;
                            }
                        }
                        if(existsLeave){
                            found = true;
                        }else{
                            // 需要修正为ready
                            PartConfig p2 = pc.clone();
                            p2.copyConf(qc);
                            p2.setStatus(PartStatus.READY);
                            ret.add(new PartOP(PartOP.MODIFY, p2));
                            found = true;
                        }
                    }
                }
            }
//            log.debug("part num {} found {}", i, found);
            if(!found){
                // 不存在， 则需要启动
                PartConfig pc = new PartConfig();
                pc.copyConf(qc);
                pc.setPartId(partid.addAndGet(1));
                pc.setQid(qc.getQid());
                pc.setPartNum(i);

                // 判断以什么状态启动
                PartConfig pa = ch.affected(pc);
                if(pa == null){
                    // 空的, 以 ready 状态启动
                    pc.setStatus(PartStatus.READY);
                    ret.add(new PartOP(PartOP.START, pc));
//                    log.debug("add new ready part {}", pc.getPartId());
                }else{
                    if(pa.getStatus() == PartStatus.LEAVE){
                        // 添加的连续分区
//                        log.warn("affected part in add {} is not ready ", pa.tag());
                        pc.setStatus(PartStatus.JOIN);
                        ret.add(new PartOP(PartOP.START, pc));
                        log.warn("start new part(join) {} ", pc.tag());
                        pa.addRelPart(pc.getPartId());
                    }else {
                        // 有被影响的节点, 以join启动
                        pc.setStatus(PartStatus.JOIN);
                        ret.add(new PartOP(PartOP.START, pc));
                        log.warn("start new part(join) {} ", pc.tag());

                        PartConfig pab = pa.clone(); //替代分区
                        pab.setPartId(partid.addAndGet(1));
                        pab.setStatus(PartStatus.JOIN);
                        ret.add(new PartOP(PartOP.START, pab));
                        log.warn("start affected part(join) {}",
                                pab.tag());

                        pa.addRelPart(pc.getPartId());
                        pa.addRelPart(pab.getPartId());
                        pa.leave();
                        //修改状态的操作, 得先看下是否已经加过了
                        boolean x = false;
                        for (PartOP po : ret) {
                            if (po.part == pa) {
                                x = true;
                                break;
                            }
                        }
                        if (!x) {
                            ret.add(new PartOP(PartOP.MODIFY, pa));
                            log.warn("change part to leave {}",
                                    pa.tag());
                        }
                    }
                }
            }
        }

        // 检查是否有多余分区(配置减少分区数)
        for(PartConfig pc: parts.values()){
            if( pc.getPartNum() <= pnum)
                continue;

            // 需要删除的分区
            if(pc.getStatus() == PartStatus.LEAVE){
                continue;
            }
            if(pc.getStatus() != PartStatus.READY) {
                log.warn("part {} will be removed not ready", pc.tag());
            }

            ch.remove(pc);

            // 检查受影响分区
            PartConfig pc1 = pc;
            int newPartId = partid.addAndGet(1); // 预先为替代分区分配id
            while(true) {
                PartConfig pa = ch.affected(pc1);
                if (pa == null) { // all parts deleted
                    log.warn("del part {} but no affected part, just change to leave", pc1.tag());
                    pc1.leave();
                    pc1.setRelatedPart(null);
                    ret.add(new PartOP(PartOP.MODIFY, pc1));
                    // DONE 前面可能有leave分区错误的关联了 newPartId, 需要删除
                    for(PartOP po: ret){
                        po.part.removeRelPart(newPartId);
                    }
                    break;
                } else {
                    if(pa.getStatus() == PartStatus.LEAVE){
                        pc1.leave();
                        pc1.setRelatedPart(pa.getRelatedPart());
                        ret.add(new PartOP(PartOP.MODIFY, pc1));
                        break;
                    }
                    if (pa.getPartNum() > pnum) {
                        pa.addRelPart(newPartId);
                        pa.leave();
                        ret.add(new PartOP(PartOP.MODIFY, pa));
                        ch.remove(pa);
                        pc1 = pa;
                    } else {
                        // 创建替代分区
                        PartConfig pab = pa.clone();
                        pab.setStatus(PartStatus.JOIN);
                        pab.setPartId(newPartId);
                        ret.add(new PartOP(PartOP.START, pab));

                        // 删除当前分区
                        pc1.leave();
                        pc1.addRelPart(newPartId);
                        ret.add(new PartOP(PartOP.MODIFY, pc1));

                        // 被影响分区删除
                        pa.leave();
                        pa.addRelPart(newPartId);
                        ret.add(new PartOP(PartOP.MODIFY, pa));
                        break;
                    }
                }
            }
        }
        for(PartOP po: ret) {
            PartConfig pc = po.part;
            if (po.optype == PartOP.MODIFY && pc.getStatus() == PartStatus.LEAVE) {
                ch.remove(pc);
            }
        }
        List<PartOP> ret1 = new LinkedList<>();
        for(PartOP po: ret) {
            PartConfig pc = po.part;
            if(po.optype == PartOP.START &&
                    (pc.getStatus() == PartStatus.JOIN || pc.getStatus() == PartStatus.READY)) {
                ch.add(pc);
                parts.put(pc.getPartId(), pc);
            }
            if(po.optype == PartOP.START)
                ret1.add(po);
        }

        return ret1;
    }


    /**
     * 定义分区操作
     */
    public static class PartOP{
        public static final int MODIFY = 91;
        public static final int START = 92;
        /**
         * 操作类型： 91-修改分区状态 92-按指定状态启动分区
         */
        public int optype;
        public PartConfig part;
        public PartOP(int optype, PartConfig part) {
            this.optype = optype;
            this.part = part;
        }

    }
    public void print(PrintStream out) {
        out.println("--circle");
        for(PartConfig pc: ch.getCircle()) {
            out.println("  "+pc.getPartId() + " " + pc.toZKString());
        }
        out.println("--all");
        for(PartConfig pc: parts.values()){
            out.println("  "+pc.getPartId() + " " + pc.toZKString());
        }
    }

    /**
     * 新增分区启动完成/或者分区状态改变完成， 对于join状态的， 需要检查是否开始修改对应的leave分区状态
     * @param partid
     *
     * @return 返回需要处理的分区任务列表
     *
     */
    public List<PartOP> partStarted(int partid) {
        List<PartOP> ret = new LinkedList<>();
        PartConfig pc = parts.get(partid);
        if(pc == null) {
            log.error("partStarted failed, part {} of {} not found", partid, qid);
            return ret;
        }
        pc.setRunStatus(PartConfig.RUN_STATUS_STARTED);

        if(pc.getStatus() == PartStatus.JOIN) {
            //检查是否有关联本分区的leave分区
            for(PartConfig pc1: parts.values()){
                if(pc1 == pc) continue;
                if(pc1.getStatus() != PartStatus.LEAVE)
                    continue;
                if(pc1.relPartExists(partid)){
                    // 有关联的leave分区

                    // 然后检查是否其所有关联分区都启动完成了
                    int[] relparts = pc1.getRelatedPart();
                    boolean all_done = true;
                    for(int pid: relparts){
                        PartConfig pc2 = parts.get(pid);
                        if(pc2.getRunStatus() != PartConfig.RUN_STATUS_STARTED){
                            all_done = false;
                            break;
                        }
                    }
                    if(all_done) {
                        pc1.setRunStatus(PartConfig.RUN_STATUS_INIT);
                        ret.add(new PartOP(PartOP.MODIFY, pc1));
                    }
                }
            }
        }
        return ret;
    }

    public PartConfig getPart(int partid) {
        return parts.get(partid);
    }

    /**
     * 检查本队列全部分区是否都已经启动完成
     * 全部启动完成需要 更新zk 的分区修改标记, 以触发broker更新分区数据
     * @return true-全启动完成
     */
    public boolean isAllPartStarted() {
        for(PartConfig pc: parts.values()){
            if(pc.getRunStatus() != PartConfig.RUN_STATUS_STARTED)
                return false;
        }
        return true;
    }

    /**
     * leave 状态分区消费完成， 检查是否修改关联join分区为ready
     * 由BLE发出的通知消息触发, BLE触发次消息前, 已主动删除该分区
     * @param partid
     *
     * @return 返回需要处理的任务列表
     */
    public List<PartOP> leaveDone(int partid) {
        // TODO leaveDone
        /* 需要处理两种情况:
         1 * leave - n * join  添加分区留下的
         n * leave - 1 * join  删除分区留下的
        * */
        List<PartOP> ret = new LinkedList<>();
        PartConfig pc = parts.get(partid);
        if(pc == null){
            log.error("leaveDone: part not found {} pid: {}", qid, partid);
            return ret;
        }

        parts.remove(partid); // 从全部分区中删除
        pc.setRunStatus(PartConfig.RUN_STATUS_LEAVEDONE); //这个语句似乎不必要了
        int[] rel = pc.getRelatedPart();
        if(rel == null){
            log.error("leave part {} related part is null", pc.tag());
            return ret;
        }
        if(rel.length == 1) {
            // 删除分区的情况, 只需要检查是否还有与之关联的leave分区
            int pid = rel[0];
            for(PartConfig pc1: parts.values()){
                if(pc1.getStatus() == PartStatus.LEAVE && pc1.existsRelPart(pid))
                    return ret; //还有未完成的分区
            }
            PartConfig pc1 = parts.get(pid);
            pc1.setStatus(PartStatus.READY);
            pc1.setRunStatus(PartConfig.RUN_STATUS_INIT);
            ret.add(new PartOP(PartOP.MODIFY, pc1));
        }else if(rel.length > 1){
            // 新增分区的情况
            for(int pid: rel){
                PartConfig pc1 = parts.get(pid);
                pc1.setStatus(PartStatus.READY);
                pc1.setRunStatus(PartConfig.RUN_STATUS_INIT);
                ret.add(new PartOP(PartOP.MODIFY, pc1));
            }
        }

        return ret;
    }

    /**
     * BLE 终止后， 为每个之前在其上承载的分区分别启动 JOIN LEAVE 替代
     * @param pc
     * @param pidSeed
     * @param ol
     */
    public void partShut(PartConfig pc, AtomicInteger pidSeed, List<PartOP> ol) {
        parts.remove(pc.getPartId());
        ch.remove(pc);

        PartConfig pcj = pc.clone();
        pcj.setRunStatus(PartConfig.RUN_STATUS_INIT);
        pcj.setStatus(PartStatus.JOIN);
        pcj.setPartId(pidSeed.addAndGet(1));
        ol.add(new PartOP(PartOP.START, pcj));

        PartConfig pcl = pc.clone();
        pcl.setRunStatus(PartConfig.RUN_STATUS_INIT);
        pcl.setStatus(PartStatus.LEAVE);
        pcl.setPartId(pidSeed.addAndGet(1));
        pcl.addRelPart(pcj.getPartId());
        ol.add(new PartOP(PartOP.START, pcl));

        parts.put(pcj.getPartId(), pcj);
        parts.put(pcl.getPartId(), pcl);
        ch.add(pcj);
    }



    /**
     * some test methods
     */
    public static void main(String[] args) {
        QueueConfig c = new QueueConfig();
        int idx = 0;
        c.setClientId("clientv2");
        c.setDestTopicId("topic");
        c.setMaxRequest(30);
        c.setMinTimeout(60);
        c.setMinTimeout(600);
        c.setConsumeSpeedLimit(0);
        c.setMaxMessages(0);
        c.setWarnMessages(0);
        c.setPartCount(10);

        AtomicInteger pid = new AtomicInteger(0);
        QueueState qs = null;
        List<PartOP> rl = null;

        int oldpart = 21, newpart = 30;
        //////////////
        ////// 增加/减少分区测试
        //////////////////
        c.setPartCount(oldpart);
        qs = new QueueState(c.getQid());
        // refresh start
        rl = qs.checkParts(pid, c);

        for(PartOP po: rl){
            qs.partStarted(po.part.getPartId());
        }

        qs.print(System.out);

        c.setPartCount(newpart);
        rl = qs.checkParts(pid, c);
        qs.print(System.out);

        // 1. 启动 join分区
        List<PartOP> l1 = new LinkedList<>();
        List<PartOP> l2 = null;
        for(PartOP po: rl){
            int partid = po.part.getPartId();
            l2 = qs.partStarted(partid);
            if(l2 != null && l2.size() > 0)
                l1.addAll((l2));
            System.out.printf("1. %d l2.size()=%d\n", partid, l2.size());
        }
        // 2. 修改状态为leave的分区
        rl = l1;
        for(PartOP po: rl) {
            qs.partStarted(po.part.getPartId());
            System.out.printf("2. %d\n", po.part.getPartId());
        }

        // 3. leave done
//        rl = l1;  //用2同样的列表来处理
        l1 = new LinkedList<>();
        for(PartOP po: rl) {
            l2 = qs.leaveDone(po.part.getPartId());
            if (l2 != null && l2.size() > 0)
                l1.addAll(l2);
        }

        // 4. change join to ready done
        rl = l1;
        for(PartOP po: rl) {
            qs.partStarted(po.part.getPartId());
        }
        qs.print(System.out);

        System.out.printf("is ready: %s\n", qs.isReady());

    }

}
