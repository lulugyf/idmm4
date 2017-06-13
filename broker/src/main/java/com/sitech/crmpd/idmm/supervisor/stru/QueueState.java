package com.sitech.crmpd.idmm.supervisor.stru;

import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartStatus;
import com.sitech.crmpd.idmm.cfg.QueueConfig;
import com.sitech.crmpd.idmm.util.BZK;
import com.sitech.crmpd.idmm.util.ch.ConsistentHash;
import com.sitech.crmpd.idmm.util.ch.StrHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    /**
     * 从BLE查询的结果更新到结构中, 构建一致性hash
     * @param pc
     */
    public void qryAdd(PartConfig pc) {
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
    public void compareZK(BZK zk){
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
        for(Integer pid: mz.keySet()){
            if(!ml.containsKey(pid)){
                changed = true;
                zk.delPart(mz.get(pid));
                mz.remove(pid);
                log.warn("compareZK remove from zk: qid: {}, partid: {}", qid, pid);
            }
        }

        //3 检查分区数据不一致
        for(PartConfig pl: ml.values()){
            PartConfig pz = mz.get(pl.getPartId());
            if(pl.getPartNum() != pz.getPartNum() ||
                    pl.getStatus() != pz.getStatus() ||
                    !pl.getBleid().equals(pz.getBleid())){
                changed = true;
                zk.setPart(pl);

                log.warn("compareZK zk ble not match, correct zk, qid: {} partid: {}",
                        qid, pl.getPartId());
            }
        }

        if(changed)
            zk.partChanged();
    }

    /**
     * 检查分区完整性
     * 检查规则:
     *     0. 基于已经存在的分区结构， 与配置的预期进行检查
     *     1. 每个分区num, 必须存在有对应ready或 join 状态分区, 否则不完整, 添加
     *     2. 每个join状态的分区, 必须有对应leave状态分区; 改为ready状态
     *     3. 多出的分区, 需进行分区的减少操作
     *     4. 重复num的分区, 回头想下该怎么搞 TODO duplicated part number? how to process
     *
     * 要新启动的分区， bleid 由后续的分配动作设定
     *
     * @param partid 分配分区id的种子, 函数返回后调用者需要检查数字是否更新过， 更新过则要把值保存到zk上
     * @param qc 配置结构体
     *
     * @return 返回需要处理的分区列表, 调用者拿到这个列表向BLE发送指令
     */
    public List<PartOP> checkParts(AtomicInteger partid, QueueConfig qc) {
        int pnum = qc.getPartCount();
        List<PartOP> ret = new LinkedList<>();

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
                }else{
                    if(pa.getStatus() != PartStatus.READY){
                        log.warn("affected part in add {} is not ready ",
                                pa.tag());
                    }
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
                    pa.setStatus(PartStatus.LEAVE);
                    //修改状态的操作, 得先看下是否已经加过了
                    boolean x = false;
                    for(PartOP po: ret){
                        if(po.part == pa){
                            x = true; break;
                        }
                    }
                    if(!x) {
                        ret.add(new PartOP(PartOP.MODIFY, pa));
                        log.warn("change part to leave {}",
                                pa.tag());
                    }
                }
            }
        }

        // 检查是否有多余分区(配置减少分区数)
        for(PartConfig pc: parts.values()){
            if( pc.getPartNum() <= pnum)
                continue;

            // 需要删除的分区
            if(pc.getStatus() != PartStatus.READY) {
                log.warn("part {} will be removed not ready", pc.tag());
            }
            if(pc.getStatus() == PartStatus.LEAVE){
                continue;
            }

            ch.remove(pc);

            // 检查受影响分区
            while(true) {
                PartConfig pa = ch.affected(pc);
                if (pa == null) {
                    log.warn("del part {} but no affected part, just change to leave", pc.tag());
                    pc.setStatus(PartStatus.LEAVE);
                    ret.add(new PartOP(PartOP.MODIFY, pc));
                    break;
                } else {
                    if (pa.getPartNum() > pnum) {
                        // 受影响的分区也超出配置最大分区数（也将被删除）， 则环上有连续要被删除的分区
                        // 考虑删除全部分区的情况
                        pa.setStatus(PartStatus.LEAVE);
                        ret.add(new PartOP(PartOP.MODIFY, pa));
                    } else {
                        // 创建替代节点
                        PartConfig pab = pa.clone();
                        pab.setStatus(PartStatus.JOIN);
                        pab.setPartId(partid.addAndGet(1));
                        ret.add(new PartOP(PartOP.START, pab));

                        // 删除当前分区
                        pc.setStatus(PartStatus.LEAVE);
                        ret.add(new PartOP(PartOP.MODIFY, pc));

                        // 被影响分区删除
                        pa.setStatus(PartStatus.LEAVE);
                        pa.addRelPart(pab.getPartId());
                        ret.add(new PartOP(PartOP.MODIFY, pa));
                        break;
                    }
                }
            }
        }
        for(PartOP po: ret) {
            PartConfig pc = po.part;
            if(po.optype == PartOP.MODIFY && pc.getStatus() == PartStatus.LEAVE) {
                ch.remove(pc);
            }
            if(po.optype == PartOP.START &&
                    (pc.getStatus() == PartStatus.JOIN || pc.getStatus() == PartStatus.READY)) {
                ch.add(pc);
                parts.put(pc.getPartId(), pc);
            }
        }

        return ret;

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

}
