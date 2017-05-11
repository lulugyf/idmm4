/**
 *
 */
package com.sitech.crmpd.idmm.client.api;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 消息类型，便于逻辑处理时易于调用对应的处理流程 <br/>
 * 仅在解包时封装到 {@link Message} 对象实例中，封包时作为框架前缀的一部分
 *
 * @author Administrator
 *
 */
public enum MessageType {

	/** 心跳：仅消息头 */
	BREAKHEART(0),
	/**
	 * 查询，仅消息头。<br/>
	 * 消息头，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * </ul>
	 */
	QUERY(1),
	/**
	 * 应答消息，仅消息头。<br/>
	 * 消息头，(*)表示可选：
	 * <ul>
	 * <li>应答返回码：{@link PropertyOption#RESULT_CODE}</li>
	 * <li>*应答返回描述：{@link PropertyOption#RESULT_DESCRIPTION}</li>
	 * <li>*代理地址：{@link PropertyOption#ADDRESS}，当应答返回码为 {@link ResultCode#OK} 且是针对
	 * {@link MessageType#QUERY} 应答时必选</li>
	 * <li>*消息标识：{@link PropertyOption#MESSAGE_ID}，当应答返回码为 {@link ResultCode#OK} 且是针对
	 * {@link MessageType#SEND} 应答时必选</li>
	 * <li>*客户端流水：{@link PropertyOption#CUSTOM_SERIAL}，当应答返回码为 {@link ResultCode#OK} 且是针对
	 * {@link MessageType#SEND} 应答时可选返回原 {@link MessageType#SEND} 消息所带该值</li>
	 * </ul>
	 */
	ANSWER(2),
	/**
	 * 发送。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * <li>主题名称：{@link PropertyOption#TOPIC}</li>
	 * <li>过期时间：{@link PropertyOption#EXPIRE_TIME}</li>
	 * <li>生效时间：{@link PropertyOption#EFFECTIVE_TIME}</li>
	 * <li>是否压缩：{@link PropertyOption#COMPRESS}</li>
	 * <li>*消息组：{@link PropertyOption#GROUP}</li>
	 * <li>*优先级：{@link PropertyOption#PRIORITY}</li>
	 * <li>生产者重发次数：{@link PropertyOption#PRODUCER_RETRY}</li>
	 * <li>*客户端流水：{@link PropertyOption#CUSTOM_SERIAL}，当生产者重发次数大于0时，该值必须和第一次发送时的值相同</li>
	 * </ul>
	 */
	SEND(11),
	/**
	 * 发送后提交。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * <li>消息标识：{@link PropertyOption#MESSAGE_ID}</li>
	 * <li>原 {@link #SEND} 消息所带属性，由Broker添加（缓存没有时从存储取）</li>
	 * <li>*计算出目标主题的key：{@link PropertyOption#CURRENT_PROPERTY_KEY}，由Broker计算添加</li>
	 * <li>*计算出目标主题的value：{@link PropertyOption#CURRENT_PROPERTY_VALUE}，由Broker从配置取值添加</li>
	 * <li>*目标主题：{@link PropertyOption#TARGET_TOPIC}，由Broker计算添加</li>
	 * </ul>
	 */
	SEND_COMMIT(12),
	/**
	 * 发送后回滚。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>消息标识：{@link PropertyOption#MESSAGE_ID}</li>
	 * </ul>
	 */
	SEND_ROLLBACK(13),
	/**
	 * 拉取。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * <li>主题名称：{@link PropertyOption#TOPIC}</li>
	 * <li>预期处理时间：{@link PropertyOption#PROCESSING_TIME}</li>
	 * <li>*应答返回码：{@link PropertyOption#RESULT_CODE}，该值表示指定消息标识的消息的处理结果： {@link ResultCode#OK}
	 * 标识commit，否则rollback</li>
	 * <li>*应答返回描述：{@link PropertyOption#RESULT_DESCRIPTION}</li>
	 * <li>*消息标识：{@link PropertyOption#MESSAGE_ID}，当存在应答返回码时必选</li>
	 * </ul>
	 */
	PULL(21),
	/**
	 * 拉取应答。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>应答返回码：{@link PropertyOption#RESULT_CODE}</li>
	 * <li>*应答返回描述：{@link PropertyOption#RESULT_DESCRIPTION}</li>
	 * <li>*原 {@link #SEND} 消息所带属性，当应答返回码为 {@link ResultCode#OK} 时必选</li>
	 * <li>*消费者重发次数：{@link PropertyOption#CONSUMER_RETRY}，当应答返回码为 {@link ResultCode#OK} 时必选</li>
	 * <li>*生产者提交时间：{@link PropertyOption#COMMIT_TIME}，当应答返回码为 {@link ResultCode#OK} 时必选，需Broker添加</li>
	 * </ul>
	 */
	PULL_ANSWER(22),
	/**
	 * 删除消息。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * <li>消息标识：{@link PropertyOption#MESSAGE_ID}</li>
	 * <li>*目标主题：{@link PropertyOption#TARGET_TOPIC}，由Broker计算添加</li>
	 * </ul>
	 */
	DELETE(30),
	/**
	 * 解锁消息。BLE之间交互消息<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link PropertyOption#CLIENT_ID}</li>
	 * <li>消息标识：{@link PropertyOption#MESSAGE_ID}</li>
	 * <li>目标主题：{@link PropertyOption#TARGET_TOPIC}</li>
	 * </ul>
	 */
	UNLOCK(40),
	/**
	 * 未定义消息，此类消息也用 {@link MessageType#ANSWER} 返回。<br/>
	 * 使用 {@link PropertyOption#RESULT_CODE} 和 {@link PropertyOption#RESULT_DESCRIPTION} 进行描述
	 */
	UNDEFINED(99);

	private static final Map<Integer, MessageType> CACHE = Maps.newHashMap();
	static {
		final MessageType[] values = values();
		for (final MessageType value : values) {
			CACHE.put(value.code, value);
		}
	}

	/**
	 * 消息类型对应的编码
	 */
	private int code;

	private MessageType(int code) {
		this.code = code;
	}

	/**
	 * 获取{@link #code}属性的值
	 *
	 * @return {@link #code}属性的值
	 */
	public int code() {
		return code;
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
	 * 获取指定的消息类型
	 *
	 * @param name
	 *            消息类型
	 * @return {@link MessageType}对象实例
	 */
	public static MessageType valueOf(Object name) {
		return name == null ? null : valueOf(name.toString());
	}

	/**
	 * 获取指定编码的消息类型
	 *
	 * @param code
	 *            消息类型编码
	 * @return {@link MessageType}对象实例
	 */
	public static MessageType valueOfCode(String code) {
		return valueOfCode(Integer.parseInt(code));
	}

	/**
	 * 获取指定编码的消息类型
	 *
	 * @param code
	 *            消息类型编码
	 * @return {@link MessageType}对象实例
	 */
	public static MessageType valueOfCode(int code) {
		if (CACHE.containsKey(code)) {
			return CACHE.get(code);
		}
		throw new IllegalArgumentException("No enum const class " + MessageType.class + ".(code="
				+ code + ")");
	}

}
