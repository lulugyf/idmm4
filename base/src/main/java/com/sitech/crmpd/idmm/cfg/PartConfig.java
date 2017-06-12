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

    /**
     * 从zk的数据字符串中解出字段内容
     * (part_num)~leave~(ble_id)[~(part_id)[,(part_id)]~(leaveTime)]
     *
     * @param s
     */
    public void fromZKString(String s) {
        String[] d = s.split("~");
        if(d.length < 3){
//            log.error("part data on zk error: {}, {}", qid, partid);
            return;
        }
        setPartNum(Integer.parseInt(d[0]));
        setStatus(PartitionStatus.valueOf(d[1]));
        setBleid(d[2]);
        if(d.length > 3){
            // 有关联分区数据
            setRelatedPart(d[3].split(","));
        }
        if(d.length > 4){
            // leave_time
            setLeaveTime(Long.parseLong(d[4]));
        }
    }

    /**
     * 拼装zk字符串
     * @return
     */
    public String toZKString() {
        char P = '~';
        StringBuilder sb = new StringBuilder();
        sb.append(partNum).append(P).append(status.toString()).append(P).append(bleid);
        if(status == PartitionStatus.LEAVE){
            sb.append(P);
            for(String r: relatedPart)
                sb.append(r).append(',');
            if(relatedPart.length > 0) sb.deleteCharAt(sb.length()-1);
            sb.append(P).append(leaveTime);
        }

        return sb.toString();
    }

    public PartConfig clone() {
        PartConfig c = new PartConfig();
        c.qid = qid;
        c.maxOnWay = maxOnWay;
        c.partNum = partNum;
        c.partId = partId;
        c.status = status;
        c.bleid = bleid;
        c.relatedPart = relatedPart;
        c.leaveTime = leaveTime;
        return c;
    }
}
