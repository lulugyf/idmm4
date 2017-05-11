/**
 *
 */
package com.sitech.crmpd.idmm.netapi;

/**
 * 框架消息，用于网络传输中的框架序列化和反序列化。<br/>
 * <br/>
 * | ①②③④  | ①②③④ | ①②③④ | ①   | ①②③     | <br/>
 *   all-len    prop-len     seq     type    reserve
 * 固定使用16字节进行消息头定义：<br/>
 * <ul>
 * <li>all-len: 消息总长度, 16头长度 + 属性json长度 + 消息体长度； 4字节big-endian</li>
 * <li>prop-len: 属性json长度, 4字节big-endian </li>
 * <li>seq: 报文序号, 4字节big-endian </li>
 * <li>type: 消息类型, 1byte </li>
 * <li>保留位：使用00占位</li>
 * </ul>
 *
 * 左补0，有利于解码时转换成数字
 *
 * @author Administrator
 *
 */
public class FramePacket extends JSONSerializable {

	/**
	 * 消息头的固定byte数组长度：{@value}
	 */
	public static final int ALL_BITS = 16;

	/**
	 * 初始化状态的消息头
	 */
	public static final byte[] INIT_HEADER = "0000000000000000".getBytes();
	/**
	 * 数字byte数组位数不够时，左补的占位(char)字符：{@value}
	 */
	public static final char ZERO = '0';


	/**
	 * 报文序号
	 */
	private int packet_seq;

	/**
	 * 消息类型
	 */
	private final FrameType type;
	/**
	 * 消息，客户端和Broker之间的消息内容。
	 */
	private final BMessage message;



	/**
	 *
	 * @param type
	 *
	 * @see #FramePacket(FrameType, BMessage)
	 */
	public FramePacket(FrameType type) {
		this(type, null);
	}

	/**
	 *
	 * @param type
	 * @param message
	 */
	public FramePacket(FrameType type, BMessage message) {
		this.type = type;
		this.message = message;
	}

	public FramePacket(FrameType type, BMessage message, int seq) {
		this.type = type;
		this.message = message;
		this.packet_seq = seq;
	}

	/**
	 * 获取{@link #type}属性的值
	 *
	 * @return {@link #type}属性的值
	 */
	public FrameType getType() {
		return type;
	}
	public int getSeq() {return packet_seq; }
	public void setSeq(int seq) { packet_seq = seq; }

	/**
	 * 获取{@link #message}属性的值
	 *
	 * @return {@link #message}属性的值
	 */
	public BMessage getMessage() {
		return message;
	}

}
