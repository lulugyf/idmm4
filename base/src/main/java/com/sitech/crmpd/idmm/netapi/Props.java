/**
 *
 */
package com.sitech.crmpd.idmm.netapi;

import com.google.common.collect.Maps;
import com.sitech.crmpd.idmm.cfg.PartStatus;

import java.util.Map;

/**
 * 消息属性的键 <br/>
 *
 * @param <T> 值类型；用来约束传入的消息属性的值类型
 * @author Administrator
 */
public final class Props<T> {

    private static final String CUSTOM_PREFIX = System.getProperty("property.option.custom.prefix",
            "custom");

    private static final Map<String, Props<?>> CACHES = Maps.newConcurrentMap();

    /**
     * 消息类型
     */
    public static final Props<FrameType> TYPE = systemValueOf("type");
    /**
     * 主题
     */
    public static final Props<String> TOPIC = systemValueOf("topic");
    /**
     * 消息组<br/>
     * 用于限定同一组消息的消费顺序
     */
    public static final Props<String> GROUP = systemValueOf("group");
    /**
     * 优先级<br/>
     * 用于限定不同组消息的消费顺序
     */
    public static final Props<Integer> PRIORITY = systemValueOf("priority");
    /**
     * 客户端标识<br/>
     * 用于标识客户端来源
     */
    public static final Props<String> CLIENT_ID = systemValueOf("client-id");
    /**
     * 目标主题
     */
    public static final Props<String> TARGET_TOPIC = systemValueOf("target-topic");

    /**
     * 队列标识: target_topic + "~" + consumer_client_id
     * broker 和 ble 内部以这个标识一个目标队列
     */
    public static final Props<String> QUEUE = systemValueOf("queue");
    /**
     * Broker地址列表
     */
    public static final Props<String[]> ADDRESS = systemValueOf("address");
    /**
     * 消息过期时间，相对于1970-1-1 00:00:00 的ms值
     */
    public static final Props<Long> EXPIRE_TIME = systemValueOf("expire-time");
    /**
     * 消息生效时间，相对于1970-1-1 00:00:00 的ms值
     */
    public static final Props<Long> EFFECTIVE_TIME = systemValueOf("effective-time");
    /**
     * 是否压缩
     */
    public static final Props<Boolean> COMPRESS = systemValueOf("compress");
    /**
     * 是否Rest方式压缩
     */
    public static final Props<Boolean> REST_COMPRESS = systemValueOf("rest-compress");
    /**
     * 是否压缩
     */
    public static final Props<Boolean> ENCRYPT = systemValueOf("encrypt");

    /**
     * 目标主题分区id
     */
    public static final Props<Integer> PRODUCER_RETRY = systemValueOf("producer-retry");

    /**
     * 中间件重试次数
     */
    public static final Props<Integer> BROKER_RETRY = systemValueOf("broker-retry");
    /**
     * 消费者重试次数
     */
    public static final Props<Integer> CONSUMER_RETRY = systemValueOf("consumer-retry");
    /**
     * 提交时间
     */
    public static final Props<Long> COMMIT_TIME = systemValueOf("commit-time");
    /**
     * 消息的唯一标识
     */
    public static final Props<String> MESSAGE_ID = systemValueOf("message-id");
    /**
     * 消息的唯一标识
     */
    public static final Props<String[]> BATCH_MESSAGE_ID = systemValueOf("batch-message-id");
    /**
     * 状态码<br/>
     * 用于承载发送服务端处理结果码
     */
    public static final Props<RetCode> RESULT_CODE = systemValueOf("ret-code");

    /**
     * 状态码、补充描述<br/>
     * 通常情况下，状态码对应的状态描述是固定的，可能会出现不同的原因导致了相同状态码，此字段用于补充描述详细信息
     */
    public static final Props<String> CODE_DESCRIPTION = systemValueOf("code-description");
    /**
     * 消费者消费消息的预期时间，超出预期时间认为消费超时
     */
    public static final Props<Long> PROCESSING_TIME = systemValueOf("processing-time");
    /**
     * 消费者失败时要求过一定时间（单位秒）后再重新处理，用于标识出不立即处理
     */
    public static final Props<Long> RETRY_AFTER = systemValueOf("retry-after");
    /**
     * 自定义流水号
     */
    public static final Props<String> CUSTOM_SERIAL = valueOf("serial");

    /**
     * 批量拉取的消息最大值
     */
    public static final Props<Integer> PAGE_SIZE = systemValueOf("page-size");
    /**
     * 客户端连接地址
     */
    public static final Props<Integer> REMOTE_ADDRESS = systemValueOf("remote-address");
    /**
     * 计算出当前目标主题的属性名
     */
    public static final Props<String> CURRENT_PROPERTY_KEY = systemValueOf("current-property-key");
    /**
     * 计算出当前目标主题的属性值（配置值，非消息所带属性值）
     */
    public static final Props<String> CURRENT_PROPERTY_VALUE = systemValueOf("current-property-value");
    /**
     * 当前主题的消费结果需要发到的主题上
     */
    public static final Props<String> REPLY_TO = systemValueOf("reply-to");
    /**
     * 标识被哪个消费者消费了
     */
    public static final Props<String> CONSUMED_BY = systemValueOf("consumed-by");
    /**
     * 生产者的消息id
     */
    public static final Props<String> PRODUCER_MESSAGE_ID = systemValueOf("producer-message-id");
    /**
     * 上一次发送产生的消息id，用于消息重发时的历史回溯
     */
    public static final Props<String> LAST_MESSAGE_ID = systemValueOf("last-message-id");
    /**
     * 客户端访问密码
     */
    public static final Props<String> VISIT_PASSWORD = systemValueOf("visit-password");


    /**
     * 应答描述信息
     */
    public static final Props<String> RESULT_DESC = systemValueOf("ret-desc");
    /**
     * 分区ID， 全局唯一的分区id， 有mgr分配
     */
    public static final Props<Integer> PART_ID = systemValueOf("part-id");
    /**
     * 分区序号, 从0开始递增的， 在同一个
     */
    public static final Props<Integer> PART_NUMBER = systemValueOf("part-num");
    /**
     * 分区状态， 1-ready 2-joining 3-leaving 9-shut
     */
    public static final Props<PartStatus> PART_STATUS = systemValueOf("part-status");
    /**
     * broker 标识id
     */
    public static final Props<String> BROKER_ID = systemValueOf("broker-id");


    /**
     * 键可视化名称
     */
    private String display;

    private Props(String display) {
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
    private synchronized static <T> Props<T> systemValueOf(String display) {
        if (!CACHES.containsKey(display)) {
            CACHES.put(display, new Props<T>(display));
        }
        return (Props<T>) CACHES.get(display);
    }

    /**
     * 创建一个指定属性名的消息属性键
     *
     * @param display 属性名
     * @return {@link Props}对象实例
     */
    @SuppressWarnings("unchecked")
    public static <T> Props<T> valueOf(String display) {
        if (!CACHES.containsKey(display)) {
            CACHES.put(display, systemValueOf(CUSTOM_PREFIX + "." + display));
        }
        return (Props<T>) CACHES.get(display);
    }
}
