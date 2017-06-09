package com.sitech.crmpd.idmm.cfg;

import com.sitech.crmpd.idmm.netapi.JSONSerializable;

/**
 * Created by guanyf on 5/8/2017.
 * 用于传输BLE的配置数据, 便于在mgr和ble间交换状态
 */
public class PartConfig extends JSONSerializable{
    private String qid;
    private int maxOnWay;
    private int partNum;
    private int partId;
    private PartitionStatus status;
    private String[] relatedPart; // leave 状态分区的关联 join 分区
    private long leaveTime;      // 状态变为 leave 的时间, 用于检测是否超时
    private String bleid;

    public String[] getRelatedPart() {
        return relatedPart;
    }

    public void setRelatedPart(String[] relatedPart) {
        this.relatedPart = relatedPart;
    }

    public long getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(long leaveTime) {
        this.leaveTime = leaveTime;
    }

    public String getBleid() {
        return bleid;
    }

    public void setBleid(String bleid) {
        this.bleid = bleid;
    }

    public String getQid() {
        return qid;
    }

    public void setQid(String qid) { this.qid = qid;    }

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
        return qid + "~" + partNum;
    }

    public PartConfig clone() {
        PartConfig c = new PartConfig();
        c.qid = qid;
        c.maxOnWay = maxOnWay;
        c.partNum = partNum;
        c.partId = partId;
        c.status = status;
        c.bleid = c.bleid;
        return c;
    }
}
