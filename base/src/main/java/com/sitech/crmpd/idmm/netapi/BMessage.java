package com.sitech.crmpd.idmm.netapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * 消息
 *
 * @author Administrator
 *
 */
@SuppressWarnings("serial")
public class BMessage extends JSONSerializable implements Serializable {
	/**
	 * 消息属性
	 */
	@JSONField(serialize = false)
	private JSONObject properties = new JSONObject(false);
	/**
	 * 系统消息属性
	 */
	private final JSONObject systemProperties = new JSONObject(false);
	/**
	 * 消息内容
	 */
	private final byte[] content;

	@JSONField(serialize = false)
	private final boolean fromString;

	/**
	 * 根据指定的属性内容和消息内容创建一个消息
	 *
	 * @param properties
	 *            属性内容
	 * @param systemProperties
	 *            系统属性内容
	 * @param content
	 *            消息内容
	 */
	private BMessage(String properties, String systemProperties, byte[] content, boolean fromString) {
		if (!Strings.isNullOrEmpty(properties)) {
			this.properties.putAll(JSON.parseObject(properties));
		}
		if (!Strings.isNullOrEmpty(systemProperties)) {
			this.systemProperties.putAll(JSON.parseObject(systemProperties));
		}
		this.content = content;
		this.fromString = fromString;
	}

	/**
	 * 创建一个未设置属性及消息内容的初始化消息
	 *
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create() {
		return new BMessage(null, null, null, false);
	}

    public static BMessage c() {
        return new BMessage(null, null, null, false);
    }

	/**
	 * 根据指定的内容创建一个消息
	 *
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(byte[] content) {
		return new BMessage(null, null, content, false);
	}

	/**
	 * 根据指定的内容创建一个消息
	 *
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(String content) {
		return new BMessage(null, null, content.getBytes(StandardCharsets.UTF_8), true);
	}

	/**
	 * 根据指定的属性创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage createSimple(byte[] properties) {
		return new BMessage(new String(properties, StandardCharsets.UTF_8), null, null, false);
	}

	/**
	 * 根据指定的属性创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage createSimple(String properties) {
		return new BMessage(properties, null, null, false);
	}

	/**
	 * 根据指定的属性和内容创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(byte[] properties, byte[] content) {
		return new BMessage(new String(properties, StandardCharsets.UTF_8), null, content, false);
	}

	/**
	 * 根据指定的属性和内容创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(byte[] properties, String content) {
		return BMessage.create(new String(properties, StandardCharsets.UTF_8), content);
	}

	/**
	 * 根据指定的属性和内容创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(String properties, String content) {
		return new BMessage(properties, null, content == null ? null
				: content.getBytes(StandardCharsets.UTF_8), true);
	}

	/**
	 * 根据指定的属性和内容创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(String properties, byte[] content) {
		return new BMessage(properties, null, content, false);
	}

	/**
	 * 根据指定的属性和内容创建一个消息
	 *
	 * @param properties
	 *            消息属性
	 * @param systemProperties
	 *            系统属性
	 * @param content
	 *            消息内容
	 * @return {@link BMessage} 对象实例
	 */
	public static BMessage create(String properties, String systemProperties, byte[] content) {
		return new BMessage(properties, systemProperties, content, false);
	}

	/**
	 * 从原始消息上复制原始属性，所复制的属性不可更改<br/>
	 * 注意：
	 * <ul>
	 * <li>只能复制原始属性，系统内部添加的属性无法复制</li>
	 * <li>复制的属性不能再重新赋值</li>
	 * </ul>
	 *
	 * @param source
	 *            原始消息
	 * @return 复制属性后的当前消息
	 */
	public BMessage copyProperties(BMessage source) {
		properties.putAll(Collections.unmodifiableMap(source.properties));
		return this;
	}

	/**
	 * 获取{@link #properties}属性的值
	 *
	 * @return {@link #properties}属性的值
	 */
	@JSONField
	private JSONObject getProperties() {
		return properties;
	}

	/**
	 * 获取{@link #properties}属性的值
	 *
	 * @return {@link #properties}属性的值
	 */
	@JSONField(name = "properties")
	public String getPropertiesAsString() {
		return JSON.toJSONString(properties, JSONSerializable.SERIALIZER_FEATURES);
	}

	/**
	 * 获取{@link #systemProperties}属性的值
	 *
	 * @return {@link #systemProperties}属性的值
	 */
	@SuppressWarnings("rawtypes")
	@JSONField(serialize = false)
	public Set<BProps> getPropertyKeys() {
		final Set<BProps> keySet = Sets.newConcurrentHashSet();
		for (final String key : properties.keySet()) {
			keySet.add(BProps.valueOf(key));
		}
		for (final String key : systemProperties.keySet()) {
			keySet.add(BProps.valueOf(key));
		}
		return keySet;
	}

	/**
	 * 获取{@link #systemProperties}属性的值
	 *
	 * @return {@link #systemProperties}属性的值
	 */
	@JSONField(serialize = false)
	public String getSystemPropertiesAsString() {
		return JSON.toJSONString(systemProperties, JSONSerializable.SERIALIZER_FEATURES);
	}

	/**
	 * @return restful格式的字符串
	 */
	public String toRestfulString() {
		final JSONObject jsonObject = (JSONObject) properties.clone();
		jsonObject.put(
				"content",
				content == null ? "" : (existProperty(BProps.COMPRESS) && getBooleanProperty(BProps.COMPRESS)) ? DatatypeConverter
						.printBase64Binary(content) : getContentAsString());
		return JSON.toJSONString(jsonObject, JSONSerializable.SERIALIZER_FEATURES);
	}

	/**
	 * 设置指定内部属性的值
	 *
	 * @param key
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 以前与 key 关联的值，如果没有针对 key 的映射关系，则返回 null。（如果该实现支持 null 值，则返回 null 也可能表示此映射以前将 null 与
	 *         key 关联）。
	 */
	@SuppressWarnings("unchecked")
	public <T> T setSystemProperty(BProps<T> key, T value) {
		return (T) systemProperties.put(key.toString(), value);
	}

	/**
	 * 删除指定的系统属性
	 *
	 * @param key
	 *            属性名
	 * @return 以前与 key 关联的值，如果没有针对 key 的映射关系，则返回 null。
	 */
	public <T> Object removeSystemProperty(BProps<T> key) {
		return systemProperties.remove(key.toString());
	}

	/**
	 * 获取指定属性的值
	 *
	 * @param key
	 *            属性名
	 * @return 属性存在返回属性值，否则返回null
	 */
	@JSONField(serialize = false)
	private Object get(BProps<?> key) {
		final String keyString = key.toString();
		return getProperty(keyString);
	}

	/**
	 * 是否存在指定的消息属性
	 *
	 * @param key
	 *            属性名
	 * @return 存在返回 <code>true</code>，否则返回 <code>false</code>
	 */
	public boolean existProperty(BProps<?> key) {
		final String keyString = key.toString();
		return existProperty(keyString);
	}

	/**
	 * 是否存在指定的消息属性
	 *
	 * @param keyString
	 *            属性名
	 * @return 存在返回 <code>true</code>，否则返回 <code>false</code>
	 */
	public boolean existProperty(String keyString) {
		return properties.containsKey(keyString) || systemProperties.containsKey(keyString);
	}

	/**
	 * 获取指定属性的值
	 *
	 * @param keyString
	 *            属性名
	 * @return 属性存在返回属性值，否则返回null
	 */
	@JSONField(serialize = false)
	public Object getProperty(String keyString) {
		if (properties.containsKey(keyString)) {
			return properties.get(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.get(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的值
	 *
	 * @param key
	 *            属性名
	 * @return 属性存在返回属性值，否则返回null
	 */
	@JSONField(serialize = false)
	public Object getProperty(BProps<?> key) {
		final Object value = get(key);
		if (value instanceof JSONArray) {
			return ((JSONArray) value).toArray();
		} else if (value instanceof JSONObject) {
			final JSONObject jsonObject = (JSONObject) value;
			return Collections.unmodifiableMap(jsonObject);
		}
		return value;
	}

	/**
	 *
	 * @param key
	 * @param type
	 * @return 枚举值
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T extends Enum> T getEnumProperty(BProps<T> key, Class<T> type) {
		final Object value = getProperty(key);
		return (T) Enum.valueOf(type, value.toString());
	}

	/**
	 * 获取指定属性的数组形式的值
	 *
	 * @param key
	 *            属性名
	 * @param a
	 *            属性值类型的数组
	 * @return 存储属性值的数组
	 */
	public <T> T[] getArray(BProps<T[]> key, T[] a) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getJSONArray(keyString).toArray(a);
		} else if (systemProperties.containsKey(keyString)) {
			return systemProperties.getJSONArray(keyString).toArray(a);
		}
		throw new NoSuchPropertyException(keyString);
	}

	public int[] getIntArray(BProps<int[]> key){
		final String keyString = key.toString();
		JSONArray ja = null;
		if (properties.containsKey(keyString)) {
			ja = properties.getJSONArray(keyString);

		} else if (systemProperties.containsKey(keyString)) {
			ja = systemProperties.getJSONArray(keyString);
		}
		if(ja != null){
			int[] r = new int[ja.size()];
			for(int i=0; i<r.length; i++)
				r[i] = (int)ja.getIntValue(i);
			return r;
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>Boolean</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Boolean</code>形式的属性值
	 */
	@JSONField(serialize = false)
	public Boolean getBooleanProperty(BProps<Boolean> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getBoolean(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getBoolean(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>boolean</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>boolean</code>形式的属性值
	 */
	public boolean getBooleanPropertyValue(BProps<Boolean> key) {
		return getBooleanProperty(key);
	}

	/**
	 * 获取指定属性的<code>byte[]</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>byte[]</code>形式的属性值
	 */
	@JSONField(serialize = false)
	public byte[] getBytesProperty(BProps<byte[]> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getBytes(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getBytes(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>Byte</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Byte</code>形式的属性值
	 */
	public Byte getByteProperty(BProps<Byte> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getByte(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getByte(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>byte</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>byte</code>形式的属性值
	 */
	public byte getBytePropertyValue(BProps<Byte> key) {
		return getByteProperty(key);
	}

	/**
	 * 获取指定属性的<code>Short</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Short</code>形式的属性值
	 */
	public Short getShortProperty(BProps<Short> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getShort(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getShort(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>short</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>short</code>形式的属性值
	 */
	public short getShortPropertyValue(BProps<Short> key) {
		return getShortProperty(key);
	}

	/**
	 * 获取指定属性的<code>Integer</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Integer</code>形式的属性值
	 */
	public Integer getIntegerProperty(BProps<Integer> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getInteger(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getInteger(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>int</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>int</code>形式的属性值
	 */
	public int getIntPropertyValue(BProps<Integer> key) {
		return getIntegerProperty(key);
	}

	/**
	 * 获取指定属性的<code>Long</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Long</code>形式的属性值
	 */
	public Long getLongProperty(BProps<Long> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getLong(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getLong(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>long</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>long</code>形式的属性值
	 */
	public long getLongPropertyValue(BProps<Long> key) {
		return getLongProperty(key);
	}

	/**
	 * 获取指定属性的<code>Float</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Float</code>形式的属性值
	 */
	public Float getFloatProperty(BProps<Float> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getFloat(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getFloat(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>float</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>float</code>形式的属性值
	 */
	public float getFloatPropertyValue(BProps<Float> key) {
		return getFloatProperty(key);
	}

	/**
	 * 获取指定属性的<code>Double</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Double</code>形式的属性值
	 */
	public Double getDoubleProperty(BProps<Double> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getDouble(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getDouble(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>double</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>double</code>形式的属性值
	 */
	public double getDoublePropertyValue(BProps<Double> key) {
		return getDoubleProperty(key);
	}

	/**
	 * 获取指定属性的<code>BigDecimal</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>BigDecimal</code>形式的属性值
	 */
	public BigDecimal getBigDecimalProperty(BProps<BigDecimal> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getBigDecimal(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getBigDecimal(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>BigInteger</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>BigInteger</code>形式的属性值
	 */
	public BigInteger getBigIntegerProperty(BProps<BigInteger> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getBigInteger(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getBigInteger(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>String</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>String</code>形式的属性值
	 */
	public String getStringProperty(BProps<?> key) {
		final String keyString = key.toString();
		return properties.containsKey(keyString) ? properties.getString(keyString)
				: systemProperties.containsKey(keyString) ? systemProperties.getString(keyString)
						: null;
	}

	/**
	 * 获取指定属性的<code>Date</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>Date</code>形式的属性值
	 */
	public Date getDateProperty(BProps<Date> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getDate(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getDate(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>java.sql.Date</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>java.sql.Date</code>形式的属性值
	 */
	public java.sql.Date getSqlDateProperty(BProps<java.sql.Date> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getSqlDate(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getSqlDate(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 获取指定属性的<code>java.sql.Timestamp</code>形式的值
	 *
	 * @param key
	 *            属性名
	 * @return <code>java.sql.Timestamp</code>形式的属性值
	 */
	public Timestamp getTimestampProperty(BProps<Timestamp> key) {
		final String keyString = key.toString();
		if (properties.containsKey(keyString)) {
			return properties.getTimestamp(keyString);
		}
		if (systemProperties.containsKey(keyString)) {
			return systemProperties.getTimestamp(keyString);
		}
		throw new NoSuchPropertyException(keyString);
	}

	/**
	 * 设置指定属性的值
	 *
	 * @param key
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 以前与 key 关联的值，如果没有针对 key 的映射关系，则返回 null。（如果该实现支持 null 值，则返回 null 也可能表示此映射以前将 null 与
	 *         key 关联）。
	 */
	@SuppressWarnings("unchecked")
	public <T> T setProperty(BProps<T> key, T value) {
		Preconditions.checkArgument(value != null);
		return (T) properties.put(key.toString(), value);
	}
	public <T> BMessage p(BProps<T> key, T value) {
		Preconditions.checkArgument(value != null);
		properties.put(key.toString(), value);
		return this;
	}
	public <T> T p(BProps<T> key) {
		try {
			return (T) get(key);
		}catch(NoSuchPropertyException e){
			return null;
		}
	}

	/**
	 * 删除指定的属性
	 *
	 * @param key
	 *            属性名
	 * @return 以前与 key 关联的值，如果没有针对 key 的映射关系，则返回 null。
	 */
	public <T> Object removeProperty(BProps<T> key) {
		return properties.remove(key.toString());
	}

	/**
	 * 获取消息内容
	 *
	 * @return 消息内容
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * 获取字符串形式的消息内容
	 *
	 * @return 字符串形式的消息内容
	 */
	@JSONField(serialize = false)
	public String getContentAsString() {
		return content == null ? null : fromString ? new String(content, StandardCharsets.UTF_8)
				: new String(content);
	}

	/**
	 * 获取UTF8编码的字符串形式的消息内容
	 *
	 * @return UTF8编码的字符串形式的消息内容
	 */
	@JSONField(serialize = false)
	public String getContentAsUtf8String() {
		return content == null ? null : new String(content, StandardCharsets.UTF_8);
	}

	/**
	 * 获取{@link }属性的值
	 *
	 * @return {@link }属性的值
	 */
	@JSONField(serialize = false)
	public String getId() {
		return getStringProperty(BProps.MESSAGE_ID);
	}

	/**
	 * 设置{@link}属性的值
	 *
	 * @param id
	 *            属性值
	 */
	public void setId(String id) {
		setProperty(BProps.MESSAGE_ID, id);
	}

}
