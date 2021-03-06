package com.sitech.crmpd.idmm.broker.config;


import com.sitech.crmpd.idmm.util.ZK;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.util.ch.ConsistentHash;
import com.sitech.crmpd.idmm.util.ch.StrHashFunction;

import java.util.Map;
import java.util.HashMap;

import java.util.List;

/**
 * Created by guanyf on 5/15/2017.
 * 给生产过程使用的分区管理类, 应用consistent-hash的地方
 *
 */
public class PartsProducer {
//    Map<String, Sub> t2s = new HashMap<>();
    private Map<String, Sub> subs = new HashMap<>();

    public static class Sub {
        private ConsistentHash<PartConfig> ch =
                new ConsistentHash<>(new StrHashFunction(), 1, null);

        private PartConfig[] num2Part; // 分区 num to id 的对应

        public void addParts(List<PartConfig> l) {
            num2Part = new PartConfig[l.size() +1];
            for(PartConfig p: l) {
                switch (p.getStatus()){
                    case READY:
                    case JOIN:
                    case SHUT:
                        ch.add(p);
                        break;
                }

                switch(p.getStatus()){
                    case READY:
                    case LEAVE:
                        num2Part[p.getPartNum()] = p;
                        break;
                }
            }
        }

        // 寻找生产者分区
        public PartConfig findPart(String group) {
            return ch.get(group);
        }
        public PartConfig getPartByNum(int partNum) {
            if(partNum > num2Part.length || partNum <= 0)
                return null;
            return num2Part[partNum];
        }

    }

    /**
     * 从zk获取全部的分区数据
     * @param zk
     */

    public void setAllParts(ZK zk, PartsConsumer cp) {
        for(String qid: zk.listQueue()) {
            Sub s = new Sub();
            List<PartConfig> pl = zk.getParts(qid);
            s.addParts(pl);
            subs.put(qid, s);

            if(cp != null)
                cp.addSub(qid, pl);
        }
    }

    /**
     * 生产者根据一致性hash确定 消息发往的分区
     * @param topic
     * @param client
     * @param group
     * @return
     */
    public PartConfig findPart(String topic, String client, String group){
        String key = topic + "~" + client;
        Sub s = subs.getOrDefault(key, null);
        if(s == null)
            return null;
        return s.findPart(group);
    }

    public PartConfig findPart(String topic, String client, int part_num) {
        String key = topic + "~" + client;
        Sub s = subs.getOrDefault(key, null);
        if(s == null)
            return null;
        return s.getPartByNum(part_num);
    }



}
