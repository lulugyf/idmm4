package com.sitech.crmpd.idmm.client.api;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月14日 下午4:10:03
 */
public enum PullCode {

	/**
	 * 提交消息
	 */
	COMMIT,
	/**
	 * 提交消息并且获取下一个消息
	 */
	COMMIT_AND_NEXT,
	/**
	 * 回滚当前消息<br/>
	 * 错误描述通过 {@link PropertyOption#CODE_DESCRIPTION} 指定<br/>
	 */
	ROLLBACK,
	/**
	 * 回滚当前消息，并跳过且不再进行当前消息的处理<br/>
	 * 错误描述通过 {@link PropertyOption#CODE_DESCRIPTION} 指定<br/>
	 */
	ROLLBACK_AND_NEXT,
	/**
	 * 回滚当前消息，并在一定时间后重试<br/>
	 * 一定时间通过 {@link PropertyOption#RETRY_AFTER} 指定<br/>
	 * 错误描述通过 {@link PropertyOption#CODE_DESCRIPTION} 指定<br/>
	 * 注：服务端锁定同group数据<br/>
	 */
	ROLLBACK_BUT_RETRY;

	/**
	 * @param name
	 *            枚举名称
	 * @return {@link PullCode} 对象实例
	 */
	public static PullCode valueOf(Object name) {
		return valueOf(name == null ? null : name.toString());
	}
}
