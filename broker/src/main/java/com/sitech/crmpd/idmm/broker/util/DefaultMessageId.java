package com.sitech.crmpd.idmm.broker.util;

import com.google.common.base.Objects;
import com.sitech.crmpd.idmm.client.api.MessageId;

/**
 * 默认消息标识符实现
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月30日 下午9:42:58
 */
public class DefaultMessageId extends MessageId {

	/**
	 * @param value
	 *            消息标识符
	 */
	public DefaultMessageId(String value) {
		super(value);
	}

	private static final long serialVersionUID = 1L;

	/**
	 * @see com.sitech.crmpd.idmm.client.api.MessageId#toString()
	 */
	@Override
	public String toString() {
		return getValue();
	}

	/**
	 * @see com.sitech.crmpd.idmm.client.api.MessageId#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MessageId)) {
			return false;
		}
		return Objects.equal(toString(), obj.toString());
	}

}
