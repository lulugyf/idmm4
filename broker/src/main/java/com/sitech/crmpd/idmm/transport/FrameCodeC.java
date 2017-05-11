package com.sitech.crmpd.idmm.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sitech.crmpd.idmm.client.api.FrameMessage;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.MessageType;
import com.sitech.crmpd.idmm.client.api.PropertyOption;

/**
 * 自定义封包解包处理类<br/>
 *
 * @see FrameMessage
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月24日 下午6:51:03
 */
public class FrameCodeC extends ByteToMessageCodec<FrameMessage> {

	/** name="{@link FrameCodeC}" */
	private static final Logger LOGGER = LoggerFactory.getLogger(FrameCodeC.class);

	/**
	 * @see io.netty.handler.codec.ByteToMessageCodec#encode(io.netty.channel.ChannelHandlerContext,
	 *      java.lang.Object, io.netty.buffer.ByteBuf)
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, FrameMessage frameMessage, ByteBuf out)
			throws Exception {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("message : {}", frameMessage);
		}
		final byte[] header = new byte[FrameMessage.INIT_HEADER.length];
		System.arraycopy(FrameMessage.INIT_HEADER, 0, header, 0, header.length);
		final Message message = frameMessage.getMessage();
		final byte[] propertiesData = message.getPropertiesAsString().getBytes();
		final byte[] content = message.getContent();
		final int bufLength = 16 + (propertiesData == null ? 0 : propertiesData.length)
				+ (content == null ? 0 : content.length);
		final byte[] allLength = Strings.padStart(String.valueOf(bufLength),
				FrameMessage.ALL_LENGTH_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(allLength, 0, header, 0, FrameMessage.ALL_LENGTH_BITS);
		final byte[] propertiesLength = Strings.padStart(
				String.valueOf(propertiesData == null ? 0 : propertiesData.length),
				FrameMessage.PROPERTIES_LENGTH_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(propertiesLength, 0, header, FrameMessage.ALL_LENGTH_BITS,
				FrameMessage.PROPERTIES_LENGTH_BITS);
		final byte[] type = Strings.padStart(String.valueOf(frameMessage.getType().code()),
				FrameMessage.TYPE_BITS, FrameMessage.ZERO).getBytes();
		System.arraycopy(type, 0, header, FrameMessage.ALL_LENGTH_BITS
				+ FrameMessage.PROPERTIES_LENGTH_BITS, FrameMessage.TYPE_BITS);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("header : {}", Arrays.toString(header));
			LOGGER.trace("properties : {}", Arrays.toString(propertiesData));
			if (content != null) {
				LOGGER.trace("content : {}", Arrays.toString(content));
			}
		}
		out.ensureWritable(bufLength);
		out.writeBytes(header);
		out.writeBytes(propertiesData);
		if (content != null) {
			out.writeBytes(content);
		}
	}

	/**
	 * @see io.netty.handler.codec.ByteToMessageCodec#decode(io.netty.channel.ChannelHandlerContext,
	 *      io.netty.buffer.ByteBuf, java.util.List)
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() < FrameMessage.ALL_BITS) {
			return;
		}
		in.markReaderIndex();
		/* 8bit */
		final byte[] allLengthData = new byte[FrameMessage.ALL_LENGTH_BITS];
		in.readBytes(allLengthData);
		final long allLength = Integer.valueOf(new String(allLengthData));
		LOGGER.trace("all-length : {}，readableBytes：{}", allLength, in.readableBytes());
		if (in.readableBytes() < allLength - FrameMessage.ALL_LENGTH_BITS) {
			in.resetReaderIndex();
			return;
		}
		/* 4bit */
		final byte[] propertiesLengthData = new byte[FrameMessage.PROPERTIES_LENGTH_BITS];
		in.readBytes(propertiesLengthData);
		final int propertiesLength = Integer.valueOf(new String(propertiesLengthData));
		LOGGER.trace("properties-length : {}", propertiesLength);
		/* 2bit */
		final byte[] typeCodeData = new byte[FrameMessage.TYPE_BITS];
		in.readBytes(typeCodeData);
		final int typeCode = Integer.valueOf(new String(typeCodeData));
		LOGGER.trace("type-code : {}", typeCode);
		final MessageType type = MessageType.valueOfCode(typeCode);
		LOGGER.trace("type : {}", type);
		if (type == MessageType.BREAKHEART) {
			/* 心跳消息，不用进行后续处理，进入下一次读取 */
			return;
		}

		in.skipBytes(FrameMessage.PERSIST_BITS);
		final byte[] propertiesData = new byte[propertiesLength];
		if (propertiesLength > 0) {
			LOGGER.trace("properties-length : {}，readableBytes：{}", propertiesData.length,
					in.readableBytes());
			in.readBytes(propertiesData);
		}
		final byte[] content = new byte[(int) (allLength - FrameMessage.ALL_BITS - propertiesLength)];
		if (content.length > 0) {
			LOGGER.trace("content-length : {}，readableBytes：{}", content.length, in.readableBytes());
			in.readBytes(content);
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("content : {}", Arrays.toString(content));
		}
		final Message message = Message.create(propertiesData, content);
		message.setProperty(PropertyOption.TYPE, type);
		out.add(new FrameMessage(type, message));
	}

}
