package com.sitech.crmpd.idmm.supervisor.stru;

import com.sitech.crmpd.idmm.cfg.PartConfig;
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
     private PartitionStatus status;
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
     *     1. 每个分区num, 必须存在有对应ready或 join 状态分区, 否则不完整, 添加
     *     2. 每个join状态分区, 必须有对应leave状态分区; 改为ready状态
     *     3. 多出的分区, 需进行分区的减少操作
     *     4. 重复num的分区, 回头想下该怎么搞 TODO duplicated part number? how to process
     * @param partid 分配分区id的种子
     * @param pnum 配置的分区数, 如果-1则不检查数量
     */
    public void checkParts(AtomicInteger partid, int pnum) {
        for(int i=1; i<=pnum; i++){

        }

    }

}
