package com.sitech.crmpd.idmm.broker.config;

import com.sitech.crmpd.idmm.client.api.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by guanyf on 6/28/2017.
 * 一套主题相关配置数据， 如果有更新的话， 就整体替换
 */
public class TopicConf {
    private Map<String, Integer> pubs; // 发布关系配置, key: client_id + '@' + src_topic_id
    private Map<String, List<TopicMapping>> topicMapping; //主题映射配置数据
    private Map<String, List<String>> subscribes;        //目标主题订阅关系配置数据

    public void loadData(Loader l) {
        pubs = new HashMap<>();
        for(String[] x: l.loadPubs()) {
            pubs.put(x[0]+"@"+x[1], 1);
        }

        topicMapping = new HashMap<>();
        for(TopicMapping t: l.loadMapping()){
            List<TopicMapping> x1 = topicMapping.getOrDefault(t.getSourceTopicId(), new LinkedList<>());
            if(x1.size() == 0) topicMapping.put(t.getSourceTopicId(), x1);
            x1.add(t);
        }

        subscribes = new HashMap<>();
        for(String[] x: l.loadSubs()){
            String clientId = x[0];
            String targetTopicId = x[1];
            List<String> x1 = subscribes.getOrDefault(targetTopicId, new LinkedList<>());
            if(x1.size() == 0) subscribes.put(targetTopicId, x1);
            x1.add(clientId);
        }
    }
    /**
     * 检查发布关系是否允许
     * @param clientId
     * @param srcTopicId
     * @return
     */
    public boolean checkPub(String clientId, String srcTopicId) {
        return pubs.containsKey(clientId + "@" + srcTopicId);
    }

    /**
     * 计算主题映射
     * @param srcTopicId
     * @param msg
     * @return 返回匹配到的目标主题+消费者 列表
     */
    public List<String[]> getMapping(String srcTopicId, Message msg) {
        return null;
    }

}
