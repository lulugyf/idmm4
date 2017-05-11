package com.sitech.crmpd.idmm.ble.mem;

import java.io.Serializable;

/**
 * 归档日志的数据元素
 * @author guanyf
 *
 */


public final class JournalOP implements Serializable {
	private static final long serialVersionUID = 893392886678252460L;
	public OP op;
	public String msgid;
	protected long maxwait;
	public MsgIndex mi;
	public String broker_id;
	protected String rcode;
	protected String desc;
	protected int retry;
	
	protected int tableid;
	protected long create_time;
	
	private static JournalOP none = new JournalOP(OP.NONE);
	
	public static JournalOP none() { return none; }
	
	public static enum OP {
		ADD,
		GET,
		COMMIT,
		ROLLBACK,
		UNLOCK,
		DEL,
		FAIL,
		DELAY,
		NONE
	}
	
	public MsgIndex getMi() {
		return mi;
	}
	
	private JournalOP(OP op){
		this.op = op;
	}
	
	protected static JournalOP add(MsgIndex mi){
		JournalOP j = new JournalOP(OP.ADD);
		j.mi = mi;
		return j;
	}
	
	protected static JournalOP get(String msgid, long maxwait, String broker_id){
		JournalOP j = new JournalOP(OP.GET);
		j.msgid = msgid;
		j.maxwait = maxwait;
		j.broker_id = broker_id;
		return j;		
	}
	
	protected static JournalOP unlock(String msgid){
		JournalOP j = new JournalOP(OP.UNLOCK);
		j.msgid = msgid;
		return j;
	}
	
	protected static JournalOP delete(String msgid){
		JournalOP j = new JournalOP(OP.DEL);
		j.msgid = msgid;
		return j;
	}
	
	protected static JournalOP commit(String msgid){
		JournalOP j = new JournalOP(OP.COMMIT);
		j.msgid = msgid;
		return j;
	}
	
	protected static JournalOP rollback(String msgid){
		JournalOP j = new JournalOP(OP.ROLLBACK);
		j.msgid = msgid;
		return j;
	}
	
	protected static JournalOP fail(MsgIndex mi, String rcode, String desc){
		JournalOP j = new JournalOP(OP.FAIL);
		j.mi = mi;
		j.msgid = mi.getMsgid();
		j.rcode = rcode;
		j.desc = desc;
		return j;
	}
	
	protected static JournalOP delay(String msgid, String rcode, long delay){
		JournalOP j = new JournalOP(OP.DELAY);
		j.msgid = msgid;
		j.rcode = rcode;
		j.maxwait = delay;
		return j;
	}

}


