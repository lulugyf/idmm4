package com.sitech.crmpd.idmm.client;

import java.io.Closeable;

import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.exception.OperationException;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月2日 下午10:04:13
 */
public abstract class MessageContext implements Closeable {
	/**
	 * 没有消息时的休眠时间
	 */
	private long noMoreMessageSleep = 1000;
	/**
	 * 客户端标识
	 */
	private final String clientId;

	protected MessageContext(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * 获取{@link #noMoreMessageSleep}属性的值
	 *
	 * @return {@link #noMoreMessageSleep}属性的值
	 */
	public long getNoMoreMessageSleep() {
		return noMoreMessageSleep;
	}

	/**
	 * 设置{@link #noMoreMessageSleep}属性的值
	 *
	 * @param noMoreMessageSleep
	 *            属性值
	 */
	public void setNoMoreMessageSleep(long noMoreMessageSleep) {
		this.noMoreMessageSleep = noMoreMessageSleep;
	}

	/**
	 * 获取{@link #clientId}属性的值
	 *
	 * @return {@link #clientId}属性的值
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * 发送一个指定主题的消息，仅用于生产者
	 *
	 * @param topic
	 *            主题名称
	 * @param message
	 *            要发送的消息
	 * @return 应答的消息
	 * @throws OperationException
	 *             发送消息过程中抛出的异常
	 */
	public abstract String send(String topic, Message message) throws OperationException;

	/**
	 * 提交一个消息，仅用于生产者
	 *
	 * @param id
	 *            消息标识符列表
	 *
	 * @throws OperationException
	 */
	public abstract void commit(String... id) throws OperationException;

	/**
	 * 回滚消息，仅用于生产者
	 *
	 * @param id
	 *            消息标识符列表
	 *
	 * @throws OperationException
	 */
	public abstract void rollback(String... id) throws OperationException;

	/**
	 * 获取一个消息，仅用于消费者
	 *
	 * @param topic
	 * @param processingTime
	 * @param lastMessageId
	 * @param lastMessageStatus
	 * @param description
	 * @return 获取的消息
	 * @throws OperationException
	 */
	public Message fetch(String topic, long processingTime, String lastMessageId,
						 PullCode lastMessageStatus, String description) throws OperationException {
		return fetch(topic, processingTime, lastMessageId, lastMessageStatus, description, true);
	}

	/**
	 * 获取一个消息，仅用于消费者
	 *
	 * @param topic
	 * @param processingTime
	 * @param lastMessageId
	 * @param lastMessageStatus
	 * @param description
	 * @param noMoreMessageBlocking
	 * @return 获取的消息
	 * @throws OperationException
	 */
	public abstract Message fetch(String topic, long processingTime, String lastMessageId,
			PullCode lastMessageStatus, String description, boolean noMoreMessageBlocking)
			throws OperationException;

	/**
	 *
	 * @param id
	 * @throws OperationException
	 */
	public abstract void delete(String... id) throws OperationException;

	/**
	 * @return 上下文环境已关闭时返回true，否则返回fasle
	 */
	public abstract boolean isClosed();
}
