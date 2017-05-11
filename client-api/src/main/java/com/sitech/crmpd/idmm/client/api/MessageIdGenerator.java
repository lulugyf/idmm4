package com.sitech.crmpd.idmm.client.api;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.google.common.base.Splitter;

/**
 * 消息唯一标识符生成器
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月5日 下午3:22:42
 */
public interface MessageIdGenerator {

	/**
	 * 消息ID各域值之间的分隔符
	 */
	public static final String SEP = "::";
	/**
	 * 消息ID各域值分割器
	 */
	public static final Splitter SPLITTER = Splitter.on(SEP).trimResults();

	/**
	 * 根据消息来源地址、消息、内部序列生成一个消息唯一标识符
	 *
	 * @param address
	 *            消息来源地址
	 * @param message
	 *            消息
	 * @param sequence
	 *            内部序列
	 * @return 消息唯一标识符
	 * @throws IOException
	 *             生成消息唯一标识符时抛出的异常
	 */
	MessageId generate(InetSocketAddress address, Message message, long sequence)
			throws IOException;

	/**
	 * 根据消息唯一标识符的字符串值还原成 {@link MessageId} 对象实例
	 *
	 * @param text
	 *            消息唯一标识符的字符串值
	 * @return {@link MessageId} 对象实例
	 * @throws IOException
	 *             还原消息唯一标识符时抛出的异常
	 */
	MessageId generate(String text) throws IOException;
}
