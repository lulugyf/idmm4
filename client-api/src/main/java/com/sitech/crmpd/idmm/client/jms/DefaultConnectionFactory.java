package com.sitech.crmpd.idmm.client.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月4日 下午4:02:18
 */
public class DefaultConnectionFactory implements ConnectionFactory {

	/**
	 * @see ConnectionFactory#createConnection()
	 */
	@Override
	public Connection createConnection() throws JMSException {
		return new DefaultConnection();
	}

	/**
	 * @see ConnectionFactory#createConnection(String, String)
	 */
	@Override
	public Connection createConnection(String userName, String password) throws JMSException {
		return new DefaultConnection(userName, password);
	}

}
