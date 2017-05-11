package com.sitech.crmpd.idmm.client.jms.message;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.Serializable;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月5日 上午10:15:09
 */
public class DefaultObjectMessage extends DefaultMessage implements ObjectMessage {

	/**
	 * @see ObjectMessage#setObject(Serializable)
	 */
	@Override
	public void setObject(Serializable object) throws JMSException {
	}

	/**
	 * @see ObjectMessage#getObject()
	 */
	@Override
	public Serializable getObject() throws JMSException {
		return null;
	}

}
