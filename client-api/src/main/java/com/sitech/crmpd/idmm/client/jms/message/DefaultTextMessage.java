package com.sitech.crmpd.idmm.client.jms.message;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月5日 上午10:15:27
 */
public class DefaultTextMessage extends DefaultMessage implements TextMessage {

	/**
	 * @see TextMessage#setText(String)
	 */
	@Override
	public void setText(String string) throws JMSException {
	}

	/**
	 * @see TextMessage#getText()
	 */
	@Override
	public String getText() throws JMSException {
		return null;
	}

}
