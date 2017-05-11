package com.sitech.crmpd.idmm.client.exception;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.sitech.crmpd.idmm.client.api.ResultCode;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月3日 下午3:52:13
 */
public class OperationException extends Exception {

	private static final long serialVersionUID = 1L;
	private final ResultCode resultCode;

	/**
	 * @param resultCode
	 */
	public OperationException(ResultCode resultCode) {
		this(resultCode, null, null);
	}

	/**
	 * @param resultCode
	 * @param message
	 */
	public OperationException(ResultCode resultCode, Object message) {
		this(resultCode, message == null ? null : message.toString(), null);
	}

	/**
	 * @param resultCode
	 * @param cause
	 */
	public OperationException(ResultCode resultCode, Throwable cause) {
		this(resultCode, null, cause);
	}

	private OperationException(ResultCode resultCode, String message, Throwable cause) {
		super(message, cause);
		this.resultCode = resultCode;
	}

	/**
	 * 获取{@link #resultCode}属性的值
	 *
	 * @return {@link #resultCode}属性的值
	 */
	public ResultCode getResultCode() {
		return resultCode;
	}

	/**
	 * @see Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return resultCode + ": " + getResultDescrition();
	}

	/**
	 * 结果描述信息
	 *
	 * @return {@link Exception#getMessage()}
	 */
	public String getResultDescrition() {
		String detailMessage = super.getMessage();
		if (Strings.isNullOrEmpty(detailMessage)) {
			final Throwable cause = getCause();
			if (cause != null) {
				detailMessage = cause.getMessage();
				if (Strings.isNullOrEmpty(detailMessage)) {
					detailMessage = Throwables.getStackTraceAsString(cause);
				}
			}
		}
		return Strings.nullToEmpty(detailMessage);
	}

}
