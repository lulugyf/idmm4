package com.sitech.crmpd.idmm.broker.config;

import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.util.BZK;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.util.ch.ConsistentHash;
import com.sitech.crmpd.idmm.util.ch.StrHashFunction;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.List;

/**
 * Created by guanyf on 5/15/2017.
 * 分区管理类
 *
 */
public class Parts {
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
                    case JOINING:
                    case SHUT:
                        ch.add(p);
                        break;
                }

                switch(p.getStatus()){
                    case READY:
                    case LEAVING:
                        num2Part[p.getPartNum()] = p;
                        break;
                    default:
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
    public void setAllParts(BZK zk, ConsumeParts cp) {
        for(String topic: zk.listTotic()) {
            Map<String, Sub> clients = new HashMap<>();
            for(String client: zk.listSubscribe(topic)){
                Sub s = new Sub();
                List<PartConfig> pl = zk.getParts(topic, client);
                s.addParts(pl);
                subs.put(topic+"~"+client, s);

                if(cp != null)
                    cp.addSub(topic, client, pl);
            }

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
