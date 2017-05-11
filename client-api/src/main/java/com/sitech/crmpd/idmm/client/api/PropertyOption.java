/**
 *
 */
package com.sitech.crmpd.idmm.client.api;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 消息属性的键 <br/>
 *
 * @author Administrator
 * @param <T>
 *            值类型；用来约束传入的消息属性的值类型
 *
 */
public final class PropertyOption<T> {

	private static final String CUSTOM_PREFIX = System.getProperty("property.option.custom.prefix",
			"custom");

	private static final Map<String, PropertyOption<?>> CACHES = Maps.newConcurrentMap();

	/**
	 * 消息类型
	 */
	public static final PropertyOption<MessageType> TYPE = systemValueOf("type");
	/**
	 * 主题
	 */
	public static final PropertyOption<String> TOPIC = systemValueOf("topic");
	/**
	 * 消息组<br/>
	 * 用于限定同一组消息的消费顺序
	 */
	public static final PropertyOption<String> GROUP = systemValueOf("group");
	/**
	 * 优先级<br/>
	 * 用于限定不同组消息的消费顺序
	 */
	public static final PropertyOption<Integer> PRIORITY = systemValueOf("priority");
	/**
	 * 客户端标识<br/>
	 * 用于标识客户端来源
	 */
	public static final PropertyOption<String> CLIENT_ID = systemValueOf("client-id");
	/**
	 * 目标主题
	 */
	public static final PropertyOption<String> TARGET_TOPIC = systemValueOf("target-topic");
	/**
	 * Broker地址列表
	 */
	public static final PropertyOption<String[]> ADDRESS = systemValueOf("address");
	/**
	 * 消息过期时间，相对于1970-1-1 00:00:00 的ms值
	 */
	public static final PropertyOption<Long> EXPIRE_TIME = systemValueOf("expire-time");
	/**
	 * 消息生效时间，相对于1970-1-1 00:00:00 的ms值
	 */
	public static final PropertyOption<Long> EFFECTIVE_TIME = systemValueOf("effective-time");
	/**
	 * 是否压缩
	 */
	public static final PropertyOption<Boolean> COMPRESS = systemValueOf("compress");
	/**
	 * 是否Rest方式压缩
	 */
	public static final PropertyOption<Boolean> REST_COMPRESS = systemValueOf("rest-compress");
	/**
	 * 是否压缩
	 */
	public static final PropertyOption<Boolean> ENCRYPT = systemValueOf("encrypt");
	/**
	 * 生产者重试次数
	 */
	public static final PropertyOption<Integer> PRODUCER_RETRY = systemValueOf("producer-retry");
	/**
	 * 中间件重试次数
	 */
	public static final PropertyOption<Integer> BROKER_RETRY = systemValueOf("broker-retry");
	/**
	 * 消费者重试次数
	 */
	public static final PropertyOption<Integer> CONSUMER_RETRY = systemValueOf("consumer-retry");
	/**
	 * 提交时间
	 */
	public static final PropertyOption<Long> COMMIT_TIME = systemValueOf("commit-time");
	/**
	 * 消息的唯一标识
	 */
	public static final PropertyOption<String> MESSAGE_ID = systemValueOf("message-id");
	/**
	 * 消息的唯一标识
	 */
	public static final PropertyOption<String[]> BATCH_MESSAGE_ID = systemValueOf("batch-message-id");
	/**
	 * 状态码<br/>
	 * 用于承载发送服务端处理结果码
	 */
	public static final PropertyOption<ResultCode> RESULT_CODE = systemValueOf("result-code");
	/**
	 * PULL消息状态码<br/>
	 * 用于承载发送消费者处理结果码
	 */
	public static final PropertyOption<PullCode> PULL_CODE = systemValueOf("pull-code");
	/**
	 * 状态码、补充描述<br/>
	 * 通常情况下，状态码对应的状态描述是固定的，可能会出现不同的原因导致了相同状态码，此字段用于补充描述详细信息
	 */
	public static final PropertyOption<String> CODE_DESCRIPTION = systemValueOf("code-description");
	/**
	 * 消费者消费消息的预期时间，超出预期时间认为消费超时
	 */
	public static final PropertyOption<Long> PROCESSING_TIME = systemValueOf("processing-time");
	/**
	 * 消费者失败时要求过一定时间（单位秒）后再重新处理，用于标识出不立即处理
	 */
	public static final PropertyOption<Long> RETRY_AFTER = systemValueOf("retry-after");
	/**
	 * 自定义流水号
	 */
	public static final PropertyOption<String> CUSTOM_SERIAL = valueOf("serial");

	/**
	 * 批量拉取的消息最大值
	 */
	public static final PropertyOption<Integer> PAGE_SIZE = systemValueOf("page-size");
	/**
	 * 客户端连接地址
	 */
	public static final PropertyOption<Integer> REMOTE_ADDRESS = systemValueOf("remote-address");
	/**
	 * 计算出当前目标主题的属性名
	 */
	public static final PropertyOption<String> CURRENT_PROPERTY_KEY = systemValueOf("current-property-key");
	/**
	 * 计算出当前目标主题的属性值（配置值，非消息所带属性值）
	 */
	public static final PropertyOption<String> CURRENT_PROPERTY_VALUE = systemValueOf("current-property-value");
	/**
	 * 当前主题的消费结果需要发到的主题上
	 */
	public static final PropertyOption<String> REPLY_TO = systemValueOf("reply-to");
	/**
	 * 标识被哪个消费者消费了
	 */
	public static final PropertyOption<String> CONSUMED_BY = systemValueOf("consumed-by");
	/**
	 * 生产者的消息id
	 */
	public static final PropertyOption<String> PRODUCER_MESSAGE_ID = systemValueOf("producer-message-id");
	/**
	 * 上一次发送产生的消息id，用于消息重发时的历史回溯
	 */
	public static final PropertyOption<String> LAST_MESSAGE_ID = systemValueOf("last-message-id");
	/**
	 * 客户端访问密码
	 */
	public static final PropertyOption<String> VISIT_PASSWORD = systemValueOf("visit-password");

	/**
	 * 键可视化名称
	 */
	private String display;

	private PropertyOption(String display) {
		super();
		this.display = display;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj || toString().equals(obj.toString()) || super.equals(obj);
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return display;
	}

	@SuppressWarnings("unchecked")
	private synchronized static <T> PropertyOption<T> systemValueOf(String display) {
		if (!CACHES.containsKey(display)) {
			CACHES.put(display, new PropertyOption<T>(display));
		}
		return (PropertyOption<T>) CACHES.get(display);
	}

	/**
	 * 创建一个指定属性名的消息属性键
	 *
	 * @param display
	 *            属性名
	 * @return {@link PropertyOption}对象实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> PropertyOption<T> valueOf(String display) {
		if (!CACHES.containsKey(display)) {
			CACHES.put(display, systemValueOf(CUSTOM_PREFIX + "." + display));
		}
		return (PropertyOption<T>) CACHES.get(display);
	}
}
