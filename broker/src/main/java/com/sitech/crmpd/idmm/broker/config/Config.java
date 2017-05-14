package com.sitech.crmpd.idmm.broker.config;


import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class Config {
    /**
     * generate test data
     * @return
     */
    public static Map<String, TopicMapping> getMap() {
        Map<String, TopicMapping> r = new HashMap<>();

        TopicMapping m = new TopicMapping();
        m.setSourceTopicId("topic");
        m.setTargetTopicId("topic");
        m.setPropertyValue("_default");

        r.put(m.getSourceTopicId(), m);

        return r;

    }
}
