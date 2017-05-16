package com.sitech.crmpd.idmm.broker.config;


import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Configuration
public class Config {
    /**
     * generate test data
     * @return
     */
    public static Map<String, List<TopicMapping>> getMap() {
        Map<String, List<TopicMapping>> r = new HashMap<>();

        TopicMapping m = new TopicMapping();
        m.setSourceTopicId("topic");
        m.setTargetTopicId("topic");
        m.setPropertyValue("_default_");

        List<TopicMapping> l = new LinkedList<>();
        l.add(m);
        r.put(m.getSourceTopicId(), l);

        return r;

    }

    public static Map<String, List<String>> getSub() {
        List<String> l = new LinkedList<>();
        l.add("client");
        Map<String, List<String>> r = new HashMap<>();
        r.put("topic", l);

        return r;
    }
}
