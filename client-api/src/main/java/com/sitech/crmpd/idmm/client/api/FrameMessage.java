/**
 *
 */
package com.sitech.crmpd.idmm.client.api;

/**
 * 框架消息，用于网络传输中的框架序列化和反序列化。<br/>
 * <br/>
 * |①②③④⑤⑥⑦⑧|①②③④|①②|①②| <br/>
 * 固定使用16字节进行消息头定义：<br/>
 * <ul>
 * <li>消息总长度：16 + 属性消息长度 + 消息内容长度；不足8位左补0</li>
 * <li>属性消息长度：消息起始位置 = 16 + 消息属性长度，消息内容长度 = 消息总长度 - 消息起始位置；不足4位左补0</li>
 * <li>消息类型：00到99</li>
 * <li>保留位：使用00占位</li>
 * </ul>
 *
 * 左补0，有利于解码时转换成数字
 *
 * @author Administrator
 *
 */
public class FrameMessage extends JSONSerializable {

	/**
	 * 消息头的固定byte数组长度：{@value}
	 */
	public static final int ALL_BITS = 16;
	/**
	 * 用于存储消息总长度的byte数组长度：{@value}
	 */
	public static final int ALL_LENGTH_BITS = 8;
	/**
	 * 用于存储属性内容总长度的byte数组长度：{@value}
	 */
	public static final int PROPERTIES_LENGTH_BITS = 4;
	/**
	 * 用于存储消息类型总长度的byte数组长度：{@value}
	 */
	public static final int TYPE_BITS = 2;
	/**
	 * 用于存储保留信息总长度的byte数组长度：{@value}
	 */
	public static final int PERSIST_BITS = 2;
	/**
	 * 初始化状态的消息头
	 */
	public static final byte[] INIT_HEADER = "0000000000000000".getBytes();
	/**
	 * 数字byte数组位数不够时，左补的占位(char)字符：{@value}
	 */
	public static final char ZERO = '0';
	/**
	 * 消息类型
	 */
	private final MessageType type;
	/**
	 * 消息，客户端和Broker之间的消息内容。
	 */
	private final Message message;

	/**
	 *
	 * @param type
	 *
	 * @see #FrameMessage(MessageType, Message)
	 */
	public FrameMessage(MessageType type) {
		this(type, null);
	}

	/**
	 *
	 * @param type
	 * @param message
	 */
	public FrameMessage(MessageType type, Message message) {
		this.type = type;
		this.message = message;
	}

	/**
	 * 获取{@link #type}属性的值
	 *
	 * @return {@link #type}属性的值
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * 获取{@link #message}属性的值
	 *
	 * @return {@link #message}属性的值
	 */
	public Message getMessage() {
		return message;
	}

}
