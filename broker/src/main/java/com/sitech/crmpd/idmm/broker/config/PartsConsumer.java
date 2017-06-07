package com.sitech.crmpd.idmm.broker.config;

import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by guanyf on 5/17/2017.
 * 给消费过程使用的分区遍历和选择
 */
public class PartsConsumer {
    private static final Logger log = LoggerFactory.getLogger(PartsConsumer.class);

    private final static String SEP = "~";
    private Map<String, SSub> subs = new HashMap<>();


    public void addSub(String topic, String client, List<PartConfig> pl) {
        String key = topic + SEP + client;
        SSub sub = new SSub(pl);
        subs.put(key, sub);
    }

    /**
     * 一个可以排序的分区结构体, 用于消费者分区扫描顺序控制
     */
    public static class SPart implements Comparable<SPart>{
        public String bleid;
        public int num;
        public int id;
        public PartitionStatus status;

        //下面是需要更新的数据, 用于综合计算后作为排序依据, 这些数据从pull的应答中获得
        protected int max_priority; // 分区积压消息的最大优先级
        protected int msg_count;    // 分区积压的消息数量
        protected int onway_left;   // 可用在途消息数

        public SPart(String bleid, int num, int id, PartitionStatus status){
            this.bleid = bleid;
            this.num = num;
            this.id = id;
            this.status = status;
        }

        @Override
        public int compareTo(SPart o) { //可能出现排序冲突 !!!
            if(max_priority > o.max_priority && msg_count > 0)
                return -1;
            if(o.max_priority > max_priority && o.msg_count > 0)
                return 1;
            return o.msg_count-msg_count; //暂时只已积压消息数排序, 从大到小排
        }
    }

    public static class SSub {
        protected List<SPart> list;
        protected SPart[] parts;
        private int cur;
        private int setCount = 0; //更新状态的次数
        public SSub(List<PartConfig> pl) {
            parts = new SPart[pl.size()+1];
            list = new ArrayList<>(pl.size());
            for(PartConfig p: pl){
                switch(p.getStatus()){
                    case READY:
                    case LEAVE: {
                        SPart s = new SPart(p.getBleid(), p.getPartNum(),
                                p.getPartId(), p.getStatus());
                        list.add(s);
                        parts[s.num] = s;
                    }
                        break;
                    default:
                        break;
                }

            }
            cur = 0;
        }
        public void setStatus(int partnum, int count, int prio, int onway){
            if(partnum >= parts.length || partnum < 1){
                log.error("setStatus: invalid partnum: {}", partnum);
                return;
            }
            SPart s = parts[partnum];

            if(s.msg_count == count)
                return;
            s.msg_count = count;
            s.max_priority = prio;
            s.onway_left = onway;
            setCount ++;

            if(setCount >= parts.length){
                setCount = 0;
                Collections.sort(list); //更新次数超过元素个数时, 重新排序
                cur = 0;
            }
        }
        public SPart nextPart() {
            if(cur >= list.size())
                cur = 0;
            return list.get(cur++);
        }
    }

    /**
     * 复制排序数据状态
     * @param cp
     */
    public void copyStatus(PartsConsumer cp) {

        for(String key: subs.keySet()){
            SSub s1 = subs.get(key);
            if(!cp.subs.containsKey(key))
                continue;
            SSub s2 = cp.subs.get(key);
            for(SPart p: s2.list){
                s1.setStatus(p.num, p.msg_count, p.max_priority, p.onway_left);
            }
        }
    }

    public SPart nextPart(String topic, String client) {
        final String key = topic + SEP + client;
        SSub s = subs.getOrDefault(key, null);
        if(s == null)
            return null;
        return s.nextPart();
    }

    public void setStatus(String topic, String client, int part_num,
                          int msg_count, int max_priority, int onway_left)
    {
        final String key = topic + SEP + client;
        SSub s = subs.getOrDefault(key, null);
        if(s == null){
            log.error("queue {} not found", key);
            return;
        }
        s.setStatus(part_num, msg_count, max_priority, onway_left);
    }
}
