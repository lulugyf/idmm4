package com.sitech.crmpd.idmm.ble.mem;

/**
 * 消息索引类型
 * @author guanyf
 *
 */
public class MsgIndex implements Comparable<MsgIndex>, java.io.Serializable{
	private static final long serialVersionUID = 5396337447400725118L;

	private String msgid;
	private String groupid;  //分组
	private int priority;    //消息优先级
	private String broker_id; //消费Broker节点id

	private long getTime;    //消息取走的时间， 如果>0 则已经取走， 还有一个超时问题， 与当前时间比较超过设定值则仍然认为锁定无效
	private long createTime; // 索引创建时间， 用于恢复内存时排序
	private int retry;
	
	private int tblid = -1; //分表标识
	private long expire_time = -1; //失效时间， unix时间戳记 ms
	
	public long getExpireTime() {
		return expire_time;
	}

	public void setExpireTime(long expire_time) {
		this.expire_time = expire_time;
	}

	private String nextTopic; //顺序消费的下一个目标主题
	private String nextClient; //顺序消费的下一个消费者
	private String produceClient; //生产者客户端
	private String srcTopic; // 原始主题

	private String commitDesc; //消费结果描述
	
	public MsgIndex(){}
	
	public MsgIndex(MsgIndex mi){
		msgid = mi.msgid;
		groupid = mi.groupid;
		priority = mi.priority;
		broker_id = mi.broker_id;
		getTime = mi.getTime;
		createTime = mi.createTime;
		retry = mi.retry;
		tblid = mi.tblid;
		nextTopic = mi.nextTopic;
		nextClient = mi.nextClient;
		produceClient = mi.produceClient;
		srcTopic = mi.srcTopic;
		commitDesc = mi.commitDesc;
	}
	
	public String getCommitDesc() {
		return commitDesc;
	}

	public void setCommitDesc(String commitDesc) {
		this.commitDesc = commitDesc;
	}

	public String getSrcTopic() {
		return srcTopic;
	}

	public void setSrcTopic(String srcTopic) {
		this.srcTopic = srcTopic;
	}

	public String getProduceClient() {
		return produceClient;
	}

	public void setProduceClient(String produceClient) {
		this.produceClient = produceClient;
	}

	public String getNextTopic() {
		return nextTopic;
	}

	public void setNextTopic(String nextTopic) {
		this.nextTopic = nextTopic;
	}

	public String getNextClient() {
		return nextClient;
	}

	public void setNextClient(String nextClient) {
		this.nextClient = nextClient;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public int getTblid() {
		return tblid;
	}

	public void setTblid(int tblid) {
		this.tblid = tblid;
	}

	public String getBroker_id() {
		return broker_id;
	}

	public void setBroker_id(String broker_id) {
		this.broker_id = broker_id;
	}
	
	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

	private Object data;

	
	public long getGetTime() {
		return getTime;
	}

	public void setGetTime(long getTime) {
		this.getTime = getTime;
	}
	
	public String getMsgid() {
		return msgid;
	}

	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}

	public String getGroupid() {
		return groupid;
	}

	public void setGroupid(String groupid) {
		this.groupid = groupid;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public int compareTo(MsgIndex o) {
		return (int)(createTime-o.createTime);
	}
	
	// 标记删除， 扫描的时候再实际删除
	public void markRemove(){
		this.getTime = -99L;
	}
	//返回是否已标记删除
	public boolean isMarkRemove(){
		return getTime == -99L;
	}

}
