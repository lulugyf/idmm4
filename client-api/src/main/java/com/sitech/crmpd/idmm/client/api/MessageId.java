package com.sitech.crmpd.idmm.client.api;

import java.io.Serializable;

/**
 * 消息唯一标识
 *
 * @author Administrator
 *
 */
public abstract class MessageId implements Serializable {

	/**
	 * 消息ID在MDC中的key
	 */
	public static final String KEY = "mid";

	protected static final long serialVersionUID = 1L;
	/**
	 * 消息唯一标识
	 */
	private final String value;

	protected MessageId(String value) {
		this.value = value;
	}

	/**
	 * 获取{@link #value}属性的值
	 *
	 * @return {@link #value}属性的值
	 */
	public String getValue() {
		return value;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see Object#toString()
	 */
	@Override
	public abstract String toString();

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public abstract boolean equals(Object obj);
}
