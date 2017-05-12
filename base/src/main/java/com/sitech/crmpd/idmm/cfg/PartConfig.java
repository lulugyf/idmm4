package com.sitech.crmpd.idmm.cfg;

import com.sitech.crmpd.idmm.netapi.JSONSerializable;

/**
 * Created by guanyf on 5/8/2017.
 * 用于传输BLE的配置数据, 便于在mgr和ble间交换状态
 */
public class PartConfig extends JSONSerializable{
    private String topicId;
    private String clientId;
    private int maxOnWay;
    private int partNum;
    private int partId;
    private PartitionStatus status;

    public String getBleid() {
        return bleid;
    }

    public void setBleid(String bleid) {
        this.bleid = bleid;
    }

    private String bleid;

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getMaxOnWay() {
        return maxOnWay;
    }

    public void setMaxOnWay(int maxOnWay) {
        this.maxOnWay = maxOnWay;
    }

    public int getPartNum() {
        return partNum;
    }

    public void setPartNum(int partNum) {
        this.partNum = partNum;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public PartitionStatus getStatus() {
        return status;
    }

    public void setStatus(PartitionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return topicId + "~" + clientId + "~" + partNum;
    }

    public PartConfig clone() {
        PartConfig c = new PartConfig();
        c.topicId = topicId;
        c.clientId = clientId;
        c.maxOnWay = maxOnWay;
        c.partNum = partNum;
        c.partId = partId;
        c.status = status;
        c.bleid = c.bleid;
        return c;
    }
}
