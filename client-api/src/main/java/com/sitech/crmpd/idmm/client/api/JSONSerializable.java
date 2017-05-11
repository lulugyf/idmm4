package com.sitech.crmpd.idmm.client.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月26日 上午9:45:48
 */
public abstract class JSONSerializable {

	/**
	 * JSON序列化选项
	 */
	public static final SerializerFeature[] SERIALIZER_FEATURES = new SerializerFeature[] {
		// SerializerFeature.NotWriteRootClassName, SerializerFeature.WriteClassName,
		SerializerFeature.DisableCheckSpecialChar, SerializerFeature.WriteSlashAsSpecial,
		SerializerFeature.WriteEnumUsingName, SerializerFeature.WriteNonStringKeyAsString,
		SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullListAsEmpty,
		SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullStringAsEmpty };

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return JSON.toJSONString(this, SERIALIZER_FEATURES);
	}
}
