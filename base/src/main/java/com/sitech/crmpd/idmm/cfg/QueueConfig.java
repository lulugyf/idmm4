package com.sitech.crmpd.idmm.cfg;

/**
 * Created by guanyf on 6/12/2017.
 * 队列配置
 */
public class QueueConfig {
    /*
    CREATE TABLE tc_topic_sub_8 (
  client_id varchar(32) NOT NULL,
  dest_topic_id varchar(32) NOT NULL,
  client_pswd char(32) DEFAULT NULL ,
  max_request number(3) DEFAULT NULL,
  min_timeout number(8) DEFAULT NULL,
  max_timeout number(8) DEFAULT NULL,
  use_status char(1) NOT NULL,
  login_no char(32) DEFAULT NULL ,
  opr_time date DEFAULT NULL,
  note varchar(2048) DEFAULT NULL ,
  consume_speed_limit number(11) DEFAULT '0',
  max_messages number(11) DEFAULT '10000',
  warn_messages number(11) DEFAULT '1000',
  PRIMARY KEY (client_id,dest_topic_id)
);

     */


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

    public String getQid() {
        return destTopicId + "~" + clientId;
    }
}
