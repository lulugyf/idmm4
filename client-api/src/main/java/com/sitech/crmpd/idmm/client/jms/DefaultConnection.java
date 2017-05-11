package com.sitech.crmpd.idmm.client.jms;

import javax.jms.*;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月4日 下午4:02:53
 */
public class DefaultConnection implements Connection {

	private String clientID;
	private String userName;
	private String password;

	public DefaultConnection() {
	}

	public DefaultConnection(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	/**
	 * @see Connection#createSession(boolean, int)
	 */
	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return new DefaultSession(transacted, acknowledgeMode);
	}

	/**
	 * @see Connection#getClientID()
	 */
	@Override
	public String getClientID() throws JMSException {
		return clientID;
	}

	/**
	 * @see Connection#setClientID(String)
	 */
	@Override
	public void setClientID(String clientID) throws JMSException {
		this.clientID = clientID;
	}

	/**
	 * @see Connection#getMetaData()
	 */
	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return null;
	}

	/**
	 * @see Connection#getExceptionListener()
	 */
	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return null;
	}

	/**
	 * @see Connection#setExceptionListener(ExceptionListener)
	 */
	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
	}

	/**
	 * @see Connection#start()
	 */
	@Override
	public void start() throws JMSException {
	}

	/**
	 * @see Connection#stop()
	 */
	@Override
	public void stop() throws JMSException {
	}

	/**
	 * @see Connection#close()
	 */
	@Override
	public void close() throws JMSException {
	}

	/**
	 * @see Connection#createConnectionConsumer(Destination, String,
	 *      ServerSessionPool, int)
	 */
	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages)
					throws JMSException {
		return null;
	}

	/**
	 * @see Connection#createDurableConnectionConsumer(Topic, String,
	 *      String, ServerSessionPool, int)
	 */
	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages)
					throws JMSException {
		return null;
	}

}
