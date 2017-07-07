package com.sitech.crmpd.idmm.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.google.common.base.Strings;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.MessageType;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.exception.OperationException;
import com.sitech.crmpd.idmm.client.utils.CodecHelper;
import com.sitech.crmpd.idmm.client.utils.SocketHelper;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月3日 上午11:04:55
 */
public final class BasicMessageContext {

	private final Socket socket;

	/**
	 * @param socket
	 *            object of socket
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @throws OperationException
	 */
	public BasicMessageContext(Socket socket, int timeout) throws OperationException {
		try {
			this.socket = create(socket, timeout);
		} catch (final IOException e) {
			throw new OperationException(ResultCode.SERVICE_ADDRESS_NOT_FOUND, e);
		}
	}

	/**
	 * @param address
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @throws OperationException
	 */
	public BasicMessageContext(InetSocketAddress address, int timeout) throws OperationException {
		try {
			socket = create(address, timeout);
		} catch (final IOException e) {
			throw new OperationException(ResultCode.SERVICE_ADDRESS_NOT_FOUND, e);
		}
	}

	/**
	 * @param host
	 *            the host name, or null for the loopback address.
	 * @param port
	 *            the port number.
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @throws OperationException
	 */
	public BasicMessageContext(String host, int port, int timeout) throws OperationException {
		try {
			socket = create(host, port, timeout);
		} catch (final IOException e) {
			throw new OperationException(ResultCode.SERVICE_ADDRESS_NOT_FOUND, e);
		}
	}

	/**
	 * 发送一个消息并接收应答消息
	 *
	 * @param clientId
	 *            客户端标识符
	 * @param type
	 *            消息类型
	 * @param message
	 *            要发送的消息
	 * @return 应答消息
	 * @throws IOException
	 *             发送和接收消息过程中抛出的异常，以及应答消息中的状态编码不为 {@link ResultCode#OK} 时抛出的异常
	 * @throws OperationException
	 *             服务端返回的异常
	 */
	public Message trade(String clientId, MessageType type, Message message)
			throws OperationException, IOException {
		try {
			message.setProperty(PropertyOption.TYPE, type);
			final Message answerMessage = CodecHelper.trade(socket, clientId, type, message);
			isOK(answerMessage);
			return answerMessage;
		} catch (final IOException e) {
			System.out.println("trade failed========="+socket.getLocalAddress().toString() + " :"+message.getId());
			closeQuietly(socket);
			throw e;
		}
	}

	private void closeQuietly(Socket socket) {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (final IOException e) {}
	}

	private void isOK(Message message) throws OperationException {
		final MessageType type = (MessageType) message.getProperty(PropertyOption.TYPE);
		try {
			if (type.name().endsWith("ANSWER")) {
				final ResultCode resultCode = ResultCode.valueOf(message
						.getProperty(PropertyOption.RESULT_CODE));
				if (resultCode != ResultCode.OK && resultCode != ResultCode.NO_MORE_MESSAGE) {
					throw new OperationException(resultCode, Strings.emptyToNull(message
							.getStringProperty(PropertyOption.CODE_DESCRIPTION)));
				}
			} else {
				throw new OperationException(ResultCode.INTERNAL_SERVICE_UNAVAILABLE,
						Strings.emptyToNull(message
								.getStringProperty(PropertyOption.CODE_DESCRIPTION)));
			}
		} catch (final OperationException e) {
			throw e;
		}
	}

	/**
	 * 是否已经关闭
	 *
	 * @return 关闭返回true，否则返回false
	 */
	public boolean isOK() {
		return !SocketHelper.isInvalid(socket);
	}

	/**
	 * 释放资源。清理Map缓存、关闭Socket连接等
	 *
	 */
	public void close() {
		closeQuietly(socket);
	}

	/**
	 * @param host
	 *            the host name, or null for the loopback address.
	 * @param port
	 *            the port number.
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @return
	 * @throws IOException
	 */
	private Socket create(String host, int port, int timeout) throws IOException {
		return create(new Socket(host, port), timeout);
	}

	/**
	 *
	 * @param address
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @return
	 * @throws IOException
	 */
	private Socket create(InetSocketAddress address, int timeout) throws IOException {
		return create(new Socket(address.getAddress(), address.getPort()), timeout);
	}

	/**
	 *
	 * @param socket
	 * @param timeout
	 *            the specified timeout, in milliseconds.
	 * @return
	 * @throws IOException
	 */
	private Socket create(Socket socket, int timeout) throws IOException {
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setSoTimeout(timeout);
		return socket;
	}

}
