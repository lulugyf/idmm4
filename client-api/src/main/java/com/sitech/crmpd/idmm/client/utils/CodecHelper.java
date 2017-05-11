package com.sitech.crmpd.idmm.client.utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import com.sitech.crmpd.idmm.client.api.PropertyOption;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月3日 上午11:36:28
 */
public final class CodecHelper {

	/** name="{@link CodecHelper}" */
	private static final Logger LOGGER = LoggerFactory.getLogger(CodecHelper.class);

	/**
	 * 将默认构造方法私有化，防止实例化后使用
	 */
	private CodecHelper() {
	}

	private static byte[] toByteArray(MessageType messageType, Message message) throws IOException {
		LOGGER.trace(">>> message : {}", message);
		final byte[] header = new byte[FrameMessage.INIT_HEADER.length];
		System.arraycopy(FrameMessage.INIT_HEADER, 0, header, 0, header.length);
		final byte[] propertiesData = message.getPropertiesAsString().getBytes();
		final byte[] content = message.getContent();
		final int allLength = 16 + (propertiesData == null ? 0 : propertiesData.length)
				+ (content == null ? 0 : content.length);
		final byte[] allLengthBits = Strings.padStart(String.valueOf(allLength),
				FrameMessage.ALL_LENGTH_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(allLengthBits, 0, header, 0, FrameMessage.ALL_LENGTH_BITS);
		final byte[] propertiesLength = Strings.padStart(
				String.valueOf(propertiesData == null ? 0 : propertiesData.length),
				FrameMessage.PROPERTIES_LENGTH_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(propertiesLength, 0, header, FrameMessage.ALL_LENGTH_BITS,
				FrameMessage.PROPERTIES_LENGTH_BITS);
		final byte[] type = Strings.padStart(String.valueOf(messageType.code()),
				FrameMessage.TYPE_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(type, 0, header, FrameMessage.ALL_LENGTH_BITS
				+ FrameMessage.PROPERTIES_LENGTH_BITS, FrameMessage.TYPE_BITS);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(">>> header : {}", Arrays.toString(header));
			LOGGER.trace(">>> properties : {}", Arrays.toString(propertiesData));
			if (content != null) {
				LOGGER.trace(">>> content : {}", Arrays.toString(content));
			}
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream(allLength)) {
			out.write(header);
			out.write(propertiesData);
			if (content != null) {
				out.write(content);
			}
			return out.toByteArray();
		} catch (final IOException e) {
			throw e;
		}
	}

	private static byte[] read(InputStream input, byte[] bytes) throws IOException {
		checkArgument(bytes != null);
		for (int i = 0; i < bytes.length; i++) {
			final int ret = input.read();
			if (ret == -1) {
				throw new EOFException();
			}
			bytes[i] = (byte) ret;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("<<< 已读取 = [{}]{}", bytes.length, Arrays.toString(bytes));
		}
		return bytes;
	}

	/**
	 * 从输入流阻塞并读取一个消息
	 *
	 * @param input
	 *            输入流
	 * @return {@link Message} 对象实例
	 * @throws IOException
	 *             IO读取时抛出的异常
	 */
	private static Message read(InputStream input) throws IOException {
		synchronized (input) {
			byte[] allBits = new byte[FrameMessage.ALL_BITS];
			allBits = read(input, allBits);
			final byte[] allLengthData = new byte[FrameMessage.ALL_LENGTH_BITS];
			System.arraycopy(allBits, 0, allLengthData, 0, allLengthData.length);
			final long allLength = Integer.valueOf(new String(allLengthData));
			LOGGER.trace("<<< all-length : {}", allLength);
			/* 4bit */
			final byte[] propertiesLengthData = new byte[FrameMessage.PROPERTIES_LENGTH_BITS];
			System.arraycopy(allBits, allLengthData.length, propertiesLengthData, 0,
					propertiesLengthData.length);
			final int propertiesLength = Integer.valueOf(new String(propertiesLengthData));
			LOGGER.trace("<<< properties-length : {}", propertiesLength);
			/* 2bit */
			final byte[] typeCodeData = new byte[FrameMessage.TYPE_BITS];
			System.arraycopy(allBits, allLengthData.length + propertiesLengthData.length,
					typeCodeData, 0, typeCodeData.length);
			final int typeCode = Integer.valueOf(new String(typeCodeData));
			LOGGER.trace("<<< type-code : {}", typeCode);
			final MessageType type = MessageType.valueOfCode(typeCode);
			LOGGER.trace("<<< type : {}", type);
			if (type == MessageType.BREAKHEART) {
				/* 心跳消息，不用进行后续处理，进入下一次读取 */
				return read(input);
			}
			byte[] propertiesData = new byte[propertiesLength];
			if (propertiesLength > 0) {
				propertiesData = read(input, propertiesData);
			}

			byte[] content = new byte[(int) (allLength - FrameMessage.ALL_BITS - propertiesLength)];
			if (content.length > 0) {
				content = read(input, content);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("<<< content : {}", Arrays.toString(content));
			}
			final Message message = Message.create(propertiesData, content);
			message.setProperty(PropertyOption.TYPE, type);
			LOGGER.trace("<<< message : {}", message);
			return message;
		}
	}

	/**
	 * 锁定 {@link Socket} 对象实例发送指定的消息并阻塞直到读取了一个返回的消息
	 *
	 * @param socket
	 *            要发送消息的 {@link Socket} 对象实例
	 * @param clientId
	 *            客户端唯一标识
	 * @param messageType
	 *            消息类型
	 * @param message
	 *            要发送的消息
	 * @return {@link Message} 对象实例
	 * @throws IOException
	 *             IO操作时抛出的异常
	 */
	public static Message trade(Socket socket, String clientId, MessageType messageType,
			Message message) throws IOException {
		if (!message.existProperty(PropertyOption.CLIENT_ID)) {
			message.setProperty(PropertyOption.CLIENT_ID, clientId);
		}
		synchronized (socket) {
			final OutputStream output = socket.getOutputStream();
			final byte[] bytes = CodecHelper.toByteArray(messageType, message);
			output.write(bytes);
			output.flush();
			final InputStream input = socket.getInputStream();
			return read(input);
		}
	}
}
