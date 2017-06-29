package com.sitech.crmpd.idmm.cfg;

/**
 * Created by guanyf on 6/12/2017.
 * 队列配置
 */
public class QueueConfig {
    private String clientId;
    private String destTopicId;
    private int maxRequest;
    private int minTimeout;
    private int maxTimeout;

    private int consumeSpeedLimit;
    private int maxMessages;
    private int warnMessages;

    /**
     * 分区数量
     */
    private int partCount;
    private int partNumStart; // 分区序号起始数字

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDestTopicId() {
        return destTopicId;
    }

    public void setDestTopicId(String destTopicId) {
        this.destTopicId = destTopicId;
    }

    public int getMaxRequest() {
        return maxRequest;
    }

    public void setMaxRequest(int maxRequest) {
        this.maxRequest = maxRequest;
    }

    public int getMinTimeout() {
        return minTimeout;
    }

    public void setMinTimeout(int minTimeout) {
        this.minTimeout = minTimeout;
    }

    public int getMaxTimeout() {
        return maxTimeout;
    }

    public void setMaxTimeout(int maxTimeout) {
        this.maxTimeout = maxTimeout;
    }

    public int getConsumeSpeedLimit() {
        return consumeSpeedLimit;
    }

    public void setConsumeSpeedLimit(int consumeSpeedLimit) {
        this.consumeSpeedLimit = consumeSpeedLimit;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public int getWarnMessages() {
        return warnMessages;
    }

    public void setWarnMessages(int warnMessages) {
        this.warnMessages = warnMessages;
    }

    public int getPartCount() {
        return partCount;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
    }


    public int getPartNumStart() {
        return partNumStart;
    }

    public void setPartNumStart(int partNumStart) {
        this.partNumStart = partNumStart;
    }

    public String getQid() {
        return destTopicId + "~" + clientId;
    }
}
