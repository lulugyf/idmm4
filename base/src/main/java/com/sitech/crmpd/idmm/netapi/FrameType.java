/**
 *
 */
package com.sitech.crmpd.idmm.netapi;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 消息类型，便于逻辑处理时易于调用对应的处理流程 <br/>
 * 仅在解包时封装到 {@link BMessage} 对象实例中，封包时作为框架前缀的一部分
 *
 * 传输中使用1字节， 最高位为1的是应答, 应答都默认必须带属性 RESULT_CODE, 可选属性 RESULT_DESC
 *
 */
public enum FrameType {

	/** 心跳：仅消息头 */
	HEARTBEAT(0x00),
	HEARTBEAT_ACK(0x80),

	/*******************
	 * mgr 向 BLE 发送的指令
	 *******************/
	/**
	 * 启动一个分区， PART_STATUS的可选值为 ready 和 joining， 状态为ready时需要从存储中恢复数据
	 * 请求参数： TARGET_TOPIC  CLIENT_ID， PART_NUM, PART_ID, PART_STATUS
	 * 		Body 中包含附加的参数, json格式, 包括最大并发数
	 */
	CMD_PT_START(0x10),
	CMD_PT_START_ACK(0x90),

	/**
	 * 修改分区状态
	 * 请求参数： PART_ID, PART_STATUS
	 */
	CMD_PT_CHANGE(0x11),
	CMD_PT_CHANGE_ACK(0x91),

	/**
	 * 查询分区状态
	 * 请求参数：
	 * 应答参数： 分区状态列表， 包含字段： TARGET_TOPIC  CLIENT_ID， PART_NUM, PART_ID, PART_STATUS, total, size
	 */
	CMD_PT_QUERY(0x12),
	CMD_PT_QUERY_ACK(0x92),



	/**********************
	 *  broker 向 ble 发送的报文
	 **********************/

	/**
	 * 建立连接， 可能需要增加一些安全校验， 暂时不实现， 只是把brokerid送过来
     * 请求参数： BROKER_ID, VISIT_PASSWORD
	 */
	BRK_CONNECT(0x20),
	BRK_CONNECT_ACK(0xa0),

    /**
     * 生产消息， 无消息body
     * 请求参数： PART_ID, BATCH_MESSAGE_ID, GROUP, PRIORITY
     */
    BRK_SEND_COMMIT(0x21),
    BRK_SEND_COMMIT_ACK(0x21|0x80),

    /**
     * pull消息，
     * 请求参数：
     * 应答参数： MESSAGE_ID CONSUMER_RETRY
     */
    BRK_PULL(0x22),
    BRK_PULL_ACK(0xa2),

    BRK_COMMIT(0x23),   // 消息处理完成 MESSAGE_ID
    BRK_COMMIT_ACK(0xa3),

    BRK_ROLLBACK(0x24), // 回滚消息 MESSAGE_ID
    BRK_ROLLBACK_ACK(0xa4),

    BRK_RETRY(0x25),    // 消息延迟重试 MESSAGE_ID RETRY_AFTER
    BRK_RETRY_ACK(0xa5),

    BRK_SKIP(0x26),     // 忽略消息 MESSAGE_ID
    BRK_SKIP_ACK(0xa6),

    /**
	 * 查询，仅消息头。<br/>
	 * 消息头，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * </ul>
	 */
	QUERY(1),
	/**
	 * 应答消息，仅消息头。<br/>
	 * 消息头，(*)表示可选：
	 * <ul>
	 * <li>应答返回码：{@link BProps#RESULT_CODE}</li>
	 * <li>*应答返回描述：{@link BProps}</li>
	 * <li>*代理地址：{@link BProps#ADDRESS}，当应答返回码为 {@link RetCode#OK} 且是针对
	 * {@link FrameType#QUERY} 应答时必选</li>
	 * <li>*消息标识：{@link BProps#MESSAGE_ID}，当应答返回码为 {@link RetCode#OK} 且是针对
	 * {@link FrameType#SEND} 应答时必选</li>
	 * <li>*客户端流水：{@link BProps#CUSTOM_SERIAL}，当应答返回码为 {@link RetCode#OK} 且是针对
	 * {@link FrameType#SEND} 应答时可选返回原 {@link FrameType#SEND} 消息所带该值</li>
	 * </ul>
	 */
	ANSWER(2),
	/**
	 * 发送。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * <li>主题名称：{@link BProps#TOPIC}</li>
	 * <li>过期时间：{@link BProps#EXPIRE_TIME}</li>
	 * <li>生效时间：{@link BProps#EFFECTIVE_TIME}</li>
	 * <li>是否压缩：{@link BProps#COMPRESS}</li>
	 * <li>*消息组：{@link BProps#GROUP}</li>
	 * <li>*优先级：{@link BProps#PRIORITY}</li>
	 * <li>生产者重发次数：{@link BProps#PRODUCER_RETRY}</li>
	 * <li>*客户端流水：{@link BProps#CUSTOM_SERIAL}，当生产者重发次数大于0时，该值必须和第一次发送时的值相同</li>
	 * </ul>
	 */
	SEND(11),
	/**
	 * 发送后提交。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * <li>消息标识：{@link BProps#MESSAGE_ID}</li>
	 * <li>原 {@link #SEND} 消息所带属性，由Broker添加（缓存没有时从存储取）</li>
	 * <li>*计算出目标主题的key：{@link BProps#CURRENT_PROPERTY_KEY}，由Broker计算添加</li>
	 * <li>*计算出目标主题的value：{@link BProps#CURRENT_PROPERTY_VALUE}，由Broker从配置取值添加</li>
	 * <li>*目标主题：{@link BProps#TARGET_TOPIC}，由Broker计算添加</li>
	 * </ul>
	 */
	SEND_COMMIT(12),
	/**
	 * 发送后回滚。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>消息标识：{@link BProps#MESSAGE_ID}</li>
	 * </ul>
	 */
	SEND_ROLLBACK(13),
	/**
	 * 拉取。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * <li>主题名称：{@link BProps#TOPIC}</li>
	 * <li>预期处理时间：{@link BProps#PROCESSING_TIME}</li>
	 * <li>*应答返回码：{@link BProps#RESULT_CODE}，该值表示指定消息标识的消息的处理结果： {@link RetCode#OK}
	 * 标识commit，否则rollback</li>
	 * <li>*应答返回描述：{@link BProps#RESULT_DESC}</li>
	 * <li>*消息标识：{@link BProps#MESSAGE_ID}，当存在应答返回码时必选</li>
	 * </ul>
	 */
	PULL(21),
	/**
	 * 拉取应答。<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>应答返回码：{@link BProps#RESULT_CODE}</li>
	 * <li>*应答返回描述：{@link BProps#RESULT_DESC}</li>
	 * <li>*原 {@link #SEND} 消息所带属性，当应答返回码为 {@link RetCode#OK} 时必选</li>
	 * <li>*消费者重发次数：{@link BProps#CONSUMER_RETRY}，当应答返回码为 {@link RetCode#OK} 时必选</li>
	 * <li>*生产者提交时间：{@link BProps#COMMIT_TIME}，当应答返回码为 {@link RetCode#OK} 时必选，需Broker添加</li>
	 * </ul>
	 */
	PULL_ANSWER(22),
	/**
	 * 删除消息。Broker做BLE计算后直接转发<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * <li>消息标识：{@link BProps#MESSAGE_ID}</li>
	 * <li>*目标主题：{@link BProps#TARGET_TOPIC}，由Broker计算添加</li>
	 * </ul>
	 */
	DELETE(30),
	/**
	 * 解锁消息。BLE之间交互消息<br/>
	 * 消息头列表，(*)表示可选：
	 * <ul>
	 * <li>客户端标识：{@link BProps#CLIENT_ID}</li>
	 * <li>消息标识：{@link BProps#MESSAGE_ID}</li>
	 * <li>目标主题：{@link BProps#TARGET_TOPIC}</li>
	 * </ul>
	 */
	UNLOCK(40),
	/**
	 * 未定义消息，此类消息也用 {@link FrameType#ANSWER} 返回。<br/>
	 * 使用 {@link BProps#RESULT_CODE} 和 {@link BProps#RESULT_DESC} 进行描述
	 */
	UNDEFINED(99);

	private static final Map<Integer, FrameType> CACHE = Maps.newHashMap();
	static {
		final FrameType[] values = values();
		for (final FrameType value : values) {
			CACHE.put(value.code, value);
		}
	}

	/**
	 * 消息类型对应的编码
	 */
	private int code;

	private FrameType(int code) {
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
	 * @return {@link FrameType}对象实例
	 */
	public static FrameType valueOf(Object name) {
		return name == null ? null : valueOf(name.toString());
	}

	/**
	 * 获取指定编码的消息类型
	 *
	 * @param code
	 *            消息类型编码
	 * @return {@link FrameType}对象实例
	 */
	public static FrameType valueOfCode(String code) {
		return valueOfCode(Integer.parseInt(code));
	}

	/**
	 * 获取指定编码的消息类型
	 *
	 * @param code
	 *            消息类型编码
	 * @return {@link FrameType}对象实例
	 */
	public static FrameType valueOfCode(int code) {
		if (CACHE.containsKey(code)) {
			return CACHE.get(code);
		}
		throw new IllegalArgumentException("No enum const class " + FrameType.class + ".(code="
				+ code + ")");
	}

}
