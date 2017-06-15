package com.sitech.crmpd.idmm.cfg;

import com.sitech.crmpd.idmm.netapi.JSONSerializable;

/**
 * Created by guanyf on 5/8/2017.
 * 用于传输BLE的配置数据, 便于在mgr和ble间交换状态
 */
public class PartConfig extends JSONSerializable{
    private String qid;
    private int partNum;
    private int partId;
    private PartStatus status;
    private int[] relatedPart; // leave 状态分区的关联 join 分区id
    private long leaveTime;      // 状态变为 leave 的时间, 用于检测是否超时
    private String bleid;

    // 从配置中继承过来的参数
    private int maxRequest;
    private int minTimeout;
    private int maxTimeout;
    private int consumeSpeedLimit;
    private int maxMessages;
    private int warnMessages;

    private int runStatus = 0; //运行状态 -1-等待关联分区启动 0-分配未启动 1-已启动 2-leave-done
    public static final int RUN_STATUS_INIT = 0; //允许执行任务
    public static final int RUN_STATUS_STARTED = 1; //执行完成
    public static final int RUN_STATUS_LEAVEDONE = 2; //leave状态分区消费完成
    public static final int RUN_STATUS_WAIT = -1; //不允许执行

    public int getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(int runStatus) {
        this.runStatus = runStatus;
    }

    public int[] getRelatedPart() {
        return relatedPart;
    }

    public void setRelatedPart(int[] relatedPart) {
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

    public int getMaxRequest() {
        return maxRequest;
    }

    public void setMaxRequest(int maxRequest) {
        this.maxRequest = maxRequest;
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

    public PartStatus getStatus() {
        return status;
    }

    public void setStatus(PartStatus status) {
        this.status = status;
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

    /**
     * this method is to ConsistentHash
     * @return
     */
    @Override
    public String toString() {
        return qid + "~" + partNum;
    }


    public void leave() {
        status = PartStatus.LEAVE;
        leaveTime = System.currentTimeMillis();
        runStatus = RUN_STATUS_WAIT;
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
        setStatus(PartStatus.valueOf(d[1]));
        setBleid(d[2]);
        if(d.length > 3){
            // 有关联分区数据
            String[] v = d[3].split(",");
            int[] r = new int[v.length];
            for(int i=0; i<v.length; i++)
                r[i] = Integer.parseInt(v[i]);
            setRelatedPart(r);
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
        sb.append(partNum).append(P).append(status.toString()).append(P).append(bleid==null?"":bleid);
        if((status == PartStatus.LEAVE || status == PartStatus.JOIN)
                && relatedPart != null){
            sb.append(P);
            for(int r: relatedPart)
                sb.append(r).append(',');
            if(relatedPart.length > 0) sb.deleteCharAt(sb.length()-1);
            sb.append(P).append(leaveTime);
        }

        return sb.toString();
    }

    public PartConfig clone() {
        PartConfig c = new PartConfig();
        c.qid = qid;
        c.maxRequest = maxRequest;
        c.partNum = partNum;
        c.partId = partId;
        c.status = status;
        c.bleid = bleid;
        c.relatedPart = relatedPart;
        c.leaveTime = leaveTime;
        c.minTimeout = minTimeout;
        c.maxTimeout = maxTimeout;
        c.consumeSpeedLimit = consumeSpeedLimit;
        c.maxMessages = maxMessages;
        c.warnMessages = warnMessages;

        return c;
    }

    /**
     * 从队列配置中copy参数
     * @param qc
     */
    public void copyConf(QueueConfig qc){
        maxRequest = qc.getMaxRequest();
        minTimeout = qc.getMinTimeout();
        maxTimeout = qc.getMaxTimeout();
        consumeSpeedLimit = qc.getConsumeSpeedLimit();
        maxMessages = qc.getMaxMessages();
        warnMessages = qc.getWarnMessages();
    }

    /**
     * 检查leave分区是否与指定分区关联
     * @param partid
     * @return
     */
    public boolean existsRelPart(int partid) {
        if(status != PartStatus.LEAVE)
            return false;
        if(relatedPart == null)
            return false;
        for(int i: relatedPart)
            if(i == partid) return true;
        return false;
    }

    /**
     * 添加关联分区
     * @param partid
     */
    public void addRelPart(int partid) {
        if(relatedPart == null)
            relatedPart = new int[]{partid};
        else{
            int[] r = new int[relatedPart.length+1];
            System.arraycopy(relatedPart, 0, r, 0, relatedPart.length);
            r[r.length-1] = partid;
            relatedPart = r;
        }
    }
    public boolean relPartExists(int partid){
        if(relatedPart == null)
            return false;
        for(int i: relatedPart){
            if(i == partid){
                return true;
            }
        }
        return false;
    }

    /**
     * 删除关联分区
     * @param partid
     * @return
     */
    public boolean removeRelPart(int partid) {
        boolean found = relPartExists(partid);
        if(found){
            int[] r = new int[relatedPart.length-1];
            int j=0;
            for(int i: relatedPart){
                if(i!=partid) r[j++] = i;
            }
            relatedPart = r;
        }
        return found;
    }

    public String tag() {
        return qid + " num:"+partNum + " id:"+partId + " "+status;
    }
}
