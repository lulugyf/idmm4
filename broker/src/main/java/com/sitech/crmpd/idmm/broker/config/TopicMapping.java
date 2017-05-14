package com.sitech.crmpd.idmm.broker.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gyf on 5/14/2017.
 */
public class TopicMapping {
    private String sourceTopicId;
    private String propertyKey;
    private String propertyValue;
    private String targetTopicId;

    public String getSourceTopicId() {
        return sourceTopicId;
    }

    public void setSourceTopicId(String sourceTopicId) {
        this.sourceTopicId = sourceTopicId;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getTargetTopicId() {
        return targetTopicId;
    }

    public void setTargetTopicId(String targetTopicId) {
        this.targetTopicId = targetTopicId;
    }

}
