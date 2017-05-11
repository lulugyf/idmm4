package com.sitech.crmpd.idmm.client.utils;

import java.io.IOException;
import java.net.Socket;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月5日 下午11:01:04
 */
public final class SocketHelper {

	/**
	 * 根据指定的地址、端口、超时时间创建一个 {@link Socket} 对象实例
	 *
	 * @param host
	 *            地址
	 * @param port
	 *            端口
	 * @param timeout
	 *            超时时间
	 * @return {@link Socket} 对象实例
	 * @throws IOException
	 *             创建 {@link Socket} 时抛出的异常
	 */
	public static Socket create(String host, int port, int timeout) throws IOException {
		final Socket socket = new Socket(host, port);
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setSoTimeout(timeout);
		return socket;
	}

	/**
	 * 根据指定的超时时间配置一个 {@link Socket} 对象实例
	 *
	 * @param socket
	 *            {@link Socket} 对象实例
	 * @param timeout
	 *            超时时间
	 * @return {@link Socket} 对象实例
	 * @throws IOException
	 *             创建 {@link Socket} 时抛出的异常
	 */
	public static Socket create(Socket socket, int timeout) throws IOException {
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setSoTimeout(timeout);
		return socket;
	}

	/**
	 * 判断指定的 {@link Socket} 对象实例是否已经无效
	 *
	 * @param socket
	 *            要判断的 {@link Socket} 对象实例
	 * @return 当对象为null 或者 {@link Socket#isClosed()} 或者 {@link Socket#isInputShutdown()}或者
	 *         {@link Socket#isOutputShutdown()} 时返回true，否则返回false
	 */
	public static boolean isInvalid(Socket socket) {
		return socket == null || socket.isClosed() || socket.isInputShutdown()
				|| socket.isOutputShutdown();
	}

	/**
	 * 关闭 {@link Socket}，且静默处理遇到的异常
	 *
	 * @param socket
	 *            指定的 {@link Socket} 对象实例
	 */
	public static void closeQuietly(Socket socket) {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (final IOException e) {}
	}
}
