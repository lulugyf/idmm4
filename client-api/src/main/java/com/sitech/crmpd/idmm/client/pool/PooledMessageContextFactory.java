package com.sitech.crmpd.idmm.client.pool;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.sitech.crmpd.idmm.client.DefaultMessageContext;
import com.sitech.crmpd.idmm.client.MessageContext;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年5月7日 下午10:35:29
 */
public class PooledMessageContextFactory implements
		KeyedPooledObjectFactory<String, MessageContext> {
	private final String addresses;
	private final int timeout;
	private String path = "/idmm/broker";
	private String tag = null;

	/**
	 * @param addresses
	 * @param timeout
	 * @throws Exception
	 */
	public PooledMessageContextFactory(String addresses, int timeout) {
		int p = addresses.indexOf('/');
		if(p > 0){
			path = addresses.substring(p);
			addresses = addresses.substring(0, p);
		}
		this.addresses = addresses;
		this.timeout = timeout;
	}

	/**
	 * @param addresses
	 * @param timeout
	 * @throws Exception
	 */
	public PooledMessageContextFactory(String addresses, int timeout, String tag) {
		int p = addresses.indexOf('/');
		if(p > 0){
			path = addresses.substring(p);
			addresses = addresses.substring(0, p);
		}
		this.addresses = addresses;
		this.timeout = timeout;
		this.tag = tag;
	}

	/**
	 * @see org.apache.commons.pool2.KeyedPooledObjectFactory#makeObject(Object)
	 */
	@Override
	public PooledObject<MessageContext> makeObject(String key) throws Exception {
		return new DefaultPooledObject<MessageContext>(new DefaultMessageContext(addresses, path,
				timeout, key, tag));
	}

	/**
	 * @see org.apache.commons.pool2.KeyedPooledObjectFactory#destroyObject(Object,
	 *      org.apache.commons.pool2.PooledObject)
	 */
	@Override
	public void destroyObject(String key, PooledObject<MessageContext> p) throws Exception {
		final MessageContext context = p.getObject();
		if(context != null){
			context.close();
		}
	}

	/**
	 * @see org.apache.commons.pool2.KeyedPooledObjectFactory#validateObject(Object,
	 *      org.apache.commons.pool2.PooledObject)
	 */
	@Override
	public boolean validateObject(String key, PooledObject<MessageContext> p) {
		final MessageContext context = p.getObject();
		return context != null;
	}

	/**
	 * @see org.apache.commons.pool2.KeyedPooledObjectFactory#activateObject(Object,
	 *      org.apache.commons.pool2.PooledObject)
	 */
	@Override
	public void activateObject(String key, PooledObject<MessageContext> p) throws Exception {
	}

	/**
	 * @see org.apache.commons.pool2.KeyedPooledObjectFactory#passivateObject(Object,
	 *      org.apache.commons.pool2.PooledObject)
	 */
	@Override
	public void passivateObject(String key, PooledObject<MessageContext> p) throws Exception {
	}

}
