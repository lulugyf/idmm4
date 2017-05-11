package com.sitech.crmpd.idmm.netapi;

/**
 * 没有找到对应属性时抛出的异常
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月1日 下午8:47:21
 */
public class NoSuchPropertyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @see RuntimeException#RuntimeException()
	 */
	public NoSuchPropertyException() {
		super();
	}

	/**
	 * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
	 */
	public NoSuchPropertyException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public NoSuchPropertyException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @see RuntimeException#RuntimeException(String)
	 */
	public NoSuchPropertyException(String message) {
		super(message);
	}

	/**
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public NoSuchPropertyException(Throwable cause) {
		super(cause);
	}

}
