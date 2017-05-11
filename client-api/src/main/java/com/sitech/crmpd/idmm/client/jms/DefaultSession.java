package com.sitech.crmpd.idmm.client.jms;

import javax.jms.*;
import java.io.Serializable;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月4日 下午4:05:26
 */
public class DefaultSession implements Session {

	private boolean transacted;
	private int acknowledgeMode;

	public DefaultSession(boolean transacted, int acknowledgeMode) {
		this.transacted = transacted;
		this.acknowledgeMode = acknowledgeMode;
	}

	/**
	 * @see Session#createBytesMessage()
	 */
	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createMapMessage()
	 */
	@Override
	public MapMessage createMapMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createMessage()
	 */
	@Override
	public Message createMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createObjectMessage()
	 */
	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createObjectMessage(Serializable)
	 */
	@Override
	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createStreamMessage()
	 */
	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createTextMessage()
	 */
	@Override
	public TextMessage createTextMessage() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createTextMessage(String)
	 */
	@Override
	public TextMessage createTextMessage(String text) throws JMSException {
		return null;
	}

	/**
	 * @see Session#getTransacted()
	 */
	@Override
	public boolean getTransacted() throws JMSException {
		return transacted;
	}

	/**
	 * @see Session#getAcknowledgeMode()
	 */
	@Override
	public int getAcknowledgeMode() throws JMSException {
		return acknowledgeMode;
	}

	/**
	 * @see Session#commit()
	 */
	@Override
	public void commit() throws JMSException {
	}

	/**
	 * @see Session#rollback()
	 */
	@Override
	public void rollback() throws JMSException {
	}

	/**
	 * @see Session#close()
	 */
	@Override
	public void close() throws JMSException {
	}

	/**
	 * @see Session#recover()
	 */
	@Override
	public void recover() throws JMSException {
	}

	/**
	 * @see Session#getMessageListener()
	 */
	@Override
	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	/**
	 * @see Session#setMessageListener(MessageListener)
	 */
	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
	}

	/**
	 * @see Session#run()
	 */
	@Override
	public void run() {
	}

	/**
	 * @see Session#createProducer(Destination)
	 */
	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createConsumer(Destination)
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createConsumer(Destination, String)
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector)
			throws JMSException {
		return null;
	}

	/**
	 * @see Session#createConsumer(Destination, String, boolean)
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector,
			boolean NoLocal) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createQueue(String)
	 */
	@Override
	public Queue createQueue(String queueName) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createTopic(String)
	 */
	@Override
	public Topic createTopic(String topicName) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createDurableSubscriber(Topic, String)
	 */
	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createDurableSubscriber(Topic, String,
	 *      String, boolean)
	 */
	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name,
			String messageSelector, boolean noLocal) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createBrowser(Queue)
	 */
	@Override
	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createBrowser(Queue, String)
	 */
	@Override
	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
		return null;
	}

	/**
	 * @see Session#createTemporaryQueue()
	 */
	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return null;
	}

	/**
	 * @see Session#createTemporaryTopic()
	 */
	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return null;
	}

	/**
	 * @see Session#unsubscribe(String)
	 */
	@Override
	public void unsubscribe(String name) throws JMSException {
	}

}
