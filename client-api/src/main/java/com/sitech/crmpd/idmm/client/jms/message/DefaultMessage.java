package com.sitech.crmpd.idmm.client.jms.message;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Enumeration;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月5日 上午10:14:15
 */
public class DefaultMessage implements Message {

	/**
	 * @see Message#getJMSMessageID()
	 */
	@Override
	public String getJMSMessageID() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setJMSMessageID(String)
	 */
	@Override
	public void setJMSMessageID(String id) throws JMSException {
	}

	/**
	 * @see Message#getJMSTimestamp()
	 */
	@Override
	public long getJMSTimestamp() throws JMSException {
		return 0;
	}

	/**
	 * @see Message#setJMSTimestamp(long)
	 */
	@Override
	public void setJMSTimestamp(long timestamp) throws JMSException {
	}

	/**
	 * @see Message#getJMSCorrelationIDAsBytes()
	 */
	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setJMSCorrelationIDAsBytes(byte[])
	 */
	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
	}

	/**
	 * @see Message#setJMSCorrelationID(String)
	 */
	@Override
	public void setJMSCorrelationID(String correlationID) throws JMSException {
	}

	/**
	 * @see Message#getJMSCorrelationID()
	 */
	@Override
	public String getJMSCorrelationID() throws JMSException {
		return null;
	}

	/**
	 * @see Message#getJMSReplyTo()
	 */
	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setJMSReplyTo(Destination)
	 */
	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
	}

	/**
	 * @see Message#getJMSDestination()
	 */
	@Override
	public Destination getJMSDestination() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setJMSDestination(Destination)
	 */
	@Override
	public void setJMSDestination(Destination destination) throws JMSException {
	}

	/**
	 * @see Message#getJMSDeliveryMode()
	 */
	@Override
	public int getJMSDeliveryMode() throws JMSException {
		return 0;
	}

	/**
	 * @see Message#setJMSDeliveryMode(int)
	 */
	@Override
	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
	}

	/**
	 * @see Message#getJMSRedelivered()
	 */
	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return false;
	}

	/**
	 * @see Message#setJMSRedelivered(boolean)
	 */
	@Override
	public void setJMSRedelivered(boolean redelivered) throws JMSException {
	}

	/**
	 * @see Message#getJMSType()
	 */
	@Override
	public String getJMSType() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setJMSType(String)
	 */
	@Override
	public void setJMSType(String type) throws JMSException {
	}

	/**
	 * @see Message#getJMSExpiration()
	 */
	@Override
	public long getJMSExpiration() throws JMSException {
		return 0;
	}

	/**
	 * @see Message#setJMSExpiration(long)
	 */
	@Override
	public void setJMSExpiration(long expiration) throws JMSException {
	}

	/**
	 * @see Message#getJMSPriority()
	 */
	@Override
	public int getJMSPriority() throws JMSException {
		return 0;
	}

	/**
	 * @see Message#setJMSPriority(int)
	 */
	@Override
	public void setJMSPriority(int priority) throws JMSException {
	}

	/**
	 * @see Message#clearProperties()
	 */
	@Override
	public void clearProperties() throws JMSException {
	}

	/**
	 * @see Message#propertyExists(String)
	 */
	@Override
	public boolean propertyExists(String name) throws JMSException {
		return false;
	}

	/**
	 * @see Message#getBooleanProperty(String)
	 */
	@Override
	public boolean getBooleanProperty(String name) throws JMSException {
		return false;
	}

	/**
	 * @see Message#getByteProperty(String)
	 */
	@Override
	public byte getByteProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getShortProperty(String)
	 */
	@Override
	public short getShortProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getIntProperty(String)
	 */
	@Override
	public int getIntProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getLongProperty(String)
	 */
	@Override
	public long getLongProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getFloatProperty(String)
	 */
	@Override
	public float getFloatProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getDoubleProperty(String)
	 */
	@Override
	public double getDoubleProperty(String name) throws JMSException {
		return 0;
	}

	/**
	 * @see Message#getStringProperty(String)
	 */
	@Override
	public String getStringProperty(String name) throws JMSException {
		return null;
	}

	/**
	 * @see Message#getObjectProperty(String)
	 */
	@Override
	public Object getObjectProperty(String name) throws JMSException {
		return null;
	}

	/**
	 * @see Message#getPropertyNames()
	 */
	@Override
	public Enumeration getPropertyNames() throws JMSException {
		return null;
	}

	/**
	 * @see Message#setBooleanProperty(String, boolean)
	 */
	@Override
	public void setBooleanProperty(String name, boolean value) throws JMSException {
	}

	/**
	 * @see Message#setByteProperty(String, byte)
	 */
	@Override
	public void setByteProperty(String name, byte value) throws JMSException {
	}

	/**
	 * @see Message#setShortProperty(String, short)
	 */
	@Override
	public void setShortProperty(String name, short value) throws JMSException {
	}

	/**
	 * @see Message#setIntProperty(String, int)
	 */
	@Override
	public void setIntProperty(String name, int value) throws JMSException {
	}

	/**
	 * @see Message#setLongProperty(String, long)
	 */
	@Override
	public void setLongProperty(String name, long value) throws JMSException {
	}

	/**
	 * @see Message#setFloatProperty(String, float)
	 */
	@Override
	public void setFloatProperty(String name, float value) throws JMSException {
	}

	/**
	 * @see Message#setDoubleProperty(String, double)
	 */
	@Override
	public void setDoubleProperty(String name, double value) throws JMSException {
	}

	/**
	 * @see Message#setStringProperty(String, String)
	 */
	@Override
	public void setStringProperty(String name, String value) throws JMSException {
	}

	/**
	 * @see Message#setObjectProperty(String, Object)
	 */
	@Override
	public void setObjectProperty(String name, Object value) throws JMSException {
	}

	/**
	 * @see Message#acknowledge()
	 */
	@Override
	public void acknowledge() throws JMSException {
	}

	/**
	 * @see Message#clearBody()
	 */
	@Override
	public void clearBody() throws JMSException {
	}

}
