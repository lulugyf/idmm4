package com.sitech.crmpd.idmm.client.jms;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import java.util.Enumeration;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月4日 下午4:09:19
 */
public class DefaultConnectionMetaData implements ConnectionMetaData {

	/**
	 * @see ConnectionMetaData#getJMSVersion()
	 */
	@Override
	public String getJMSVersion() throws JMSException {
		return "1.1";
	}

	/**
	 * @see ConnectionMetaData#getJMSMajorVersion()
	 */
	@Override
	public int getJMSMajorVersion() throws JMSException {
		return 1;
	}

	/**
	 * @see ConnectionMetaData#getJMSMinorVersion()
	 */
	@Override
	public int getJMSMinorVersion() throws JMSException {
		return 1;
	}

	/**
	 * @see ConnectionMetaData#getJMSProviderName()
	 */
	@Override
	public String getJMSProviderName() throws JMSException {
		return null;
	}

	/**
	 * @see ConnectionMetaData#getProviderVersion()
	 */
	@Override
	public String getProviderVersion() throws JMSException {
		return "2.0";
	}

	/**
	 * @see ConnectionMetaData#getProviderMajorVersion()
	 */
	@Override
	public int getProviderMajorVersion() throws JMSException {
		return 2;
	}

	/**
	 * @see ConnectionMetaData#getProviderMinorVersion()
	 */
	@Override
	public int getProviderMinorVersion() throws JMSException {
		return 0;
	}

	/**
	 * @see ConnectionMetaData#getJMSXPropertyNames()
	 */
	@Override
	public Enumeration getJMSXPropertyNames() throws JMSException {
		return null;
	}

}
