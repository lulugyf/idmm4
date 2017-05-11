/**
 *
 */
package com.sitech.crmpd.idmm.client.api;

import java.io.IOException;

/**
 * Broker地址查询接口
 *
 * @author Administrator
 *
 */
public interface AddressQuery {

	/**
	 * 地址查询接口
	 *
	 * @param address
	 *            默认Broker地址列表
	 * @return 全量的Broker地址列表
	 * @throws IOException
	 *             获取全量的Broker地址列表时抛出的异常
	 */
	String[] query(String address) throws IOException;
}
