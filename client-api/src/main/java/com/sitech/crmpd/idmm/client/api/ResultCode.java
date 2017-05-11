package com.sitech.crmpd.idmm.client.api;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 应答返回码。定义了已知的应答返回码<br/>
 * <ul>
 * <li>0000：固定成功</li>
 * <li>0XXX：非错误类指示消息，可用来表示没有可用数据等等</li>
 * <li>1XXX：客户端问题导致的错误</li>
 * <li>2XXX：服务端问题导致的错误</li>
 * </ul>
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月12日 下午4:08:35
 */
public enum ResultCode {
	/**
	 * 成功
	 */
	OK("0000"),
	/**
	 * pull无更多消息
	 */
	NO_MORE_MESSAGE("0001"),
	/**
	 * 没有可用服务端地址，需要客户端检查地址配置
	 */
	SERVER_NOT_AVAILABLE("1000"),
	/**
	 * 负载地址无法获取，需要客户端检查地址配置
	 */
	SERVICE_ADDRESS_NOT_FOUND("1001"),
	/**
	 * 参数缺少，需要客户端检查请求参数
	 */
	REQUIRED_PARAMETER_MISSING("1002"),
	/**
	 * 错误的请求，消息类型错误，消息字段缺少时等等场景使用
	 */
	BAD_REQUEST("1003"),
	/**
	 * 请求消息类型错误
	 */
	UNSUPPORTED_MESSAGE_TYPE("1004"),
	/**
	 * 请求消息长度过长
	 */
	MESSAGE_CONTENT_LENGTH_IS_TOO_LANG("1005"),
	/**
	 * 服务器遇到了一个未曾预料的状况，导致了它无法完成对请求的处理。<br/>
	 * 一般来说，这个问题都会在服务器的程序码出错时出现。<br/>
	 * 此时客户端之后的重试无意义，会有同样的错误
	 */
	INTERNAL_SERVER_ERROR("2000"),
	/**
	 * 服务器上数据库相关操作发生了异常
	 */
	INTERNAL_DATA_ACCESS_EXCEPTION("2001"),
	/**
	 * 由于临时的服务器维护或者过载，服务器当前无法处理请求。<br/>
	 * 这个状况是临时的，并且将在一段时间以后恢复。<br/>
	 * 和 {@link ResultCode#INTERNAL_SERVER_ERROR} 不同的是客户端之后的重试都是有意义的。<br/>
	 * 客户端API中物理链路上发生的超时、断链、不可写、不可读等等状态都可以表示
	 */
	INTERNAL_SERVICE_UNAVAILABLE("2002");

	/**
	 * 应答返回码对应的编码
	 */
	private String code;
	private static final Map<String, ResultCode> CACHE = Maps.newHashMap();

	private ResultCode(String code) {
		this.code = code;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see Enum#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder(super.toString()).append("(").append(code).append(")").toString();
	}

	/**
	 * @param name
	 * @return {@link ResultCode} 对象实例
	 */
	public static ResultCode valueOf(Object name) {
		return valueOf(name == null ? null : name.toString());
	}

	/**
	 * 获取指定编码的应答返回码
	 *
	 * @param code
	 *            应答返回码编码
	 * @return {@link ResultCode}对象实例
	 */
	public static ResultCode valueOfCode(String code) {
		synchronized (CACHE) {
			if (CACHE.isEmpty()) {
				final ResultCode[] values = values();
				for (final ResultCode value : values) {
					CACHE.put(value.code, value);
				}
			}
		}
		if (CACHE.containsKey(code)) {
			return CACHE.get(code);
		}
		throw new IllegalArgumentException("No enum const class " + ResultCode.class + ".(code="
				+ code + ")");
	}

	/**
	 *
	 * @param args
	 *            入参
	 */
	public static void main(String[] args) {
		System.out.println(ResultCode.valueOfCode("0"));
		System.out.println(ResultCode.valueOfCode("010"));
	}
}
