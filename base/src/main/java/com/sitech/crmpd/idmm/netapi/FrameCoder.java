package com.sitech.crmpd.idmm.netapi;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义封包解包处理类<br/>
 *
 * @see FramePacket
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月24日 下午6:51:03
 */
public class FrameCoder extends ByteToMessageCodec<FramePacket> {

	/** name="{@link FrameCoder}" */
	private static final Logger LOGGER = LoggerFactory.getLogger(FrameCoder.class);

	// 0xAABBCCDD => {AA, BB, CC, DD}
	private static final void toBytes(int i, byte[] buf, int start) {
		buf[start+0] = (byte) (i >> 24);
		buf[start+1] = (byte) (i >> 16);
		buf[start+2] = (byte) (i >> 8);
		buf[start+3] = (byte) (i /*>> 0*/);
	}

	private static final int fromBytes(byte[] buf, int start) {
		return  ((buf[start+0] & 0xff) << 24) |
				((buf[start+1] & 0xff) << 16) |
				((buf[start+2] & 0xff) << 8) |
				(buf[start+3] & 0xff);
	}

	/**
	 * @see ByteToMessageCodec#encode(ChannelHandlerContext,
	 *      Object, ByteBuf)
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, FramePacket frameMessage, ByteBuf out)
			throws Exception {

//		LOGGER.info("encode--message : {}", frameMessage);

		final byte[] header = new byte[FramePacket.INIT_HEADER.length];
		System.arraycopy(FramePacket.INIT_HEADER, 0, header, 0, header.length);
		final BMessage message = frameMessage.getMessage();
		final byte[] propertiesData = message.getPropertiesAsString().getBytes();
		final byte[] content = message.getContent();
		final int bufLength = 16 + (propertiesData == null ? 0 : propertiesData.length)
				+ (content == null ? 0 : content.length);

		toBytes(bufLength, header, 0);
		final int propertyLen = propertiesData == null ? 0 : propertiesData.length;
		toBytes(propertyLen, header, 4);
		toBytes(frameMessage.getSeq(), header, 8);
		header[12] = (byte)(frameMessage.getType().code() & 0xff);
//		LOGGER.info("all-len:{} prop-len:{}", bufLength, propertyLen);

//		final byte[] type = Strings.padStart(String.valueOf(frameMessage.getType().code()),
//				FramePacket.TYPE_BITS, FramePacket.ZERO).getBytes();
//		System.arraycopy(type, 0, header, FramePacket.ALL_LENGTH_BITS
//				+ FramePacket.PROPERTIES_LENGTH_BITS, FramePacket.TYPE_BITS);
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
	 * @see ByteToMessageCodec#decode(ChannelHandlerContext,
	 *      ByteBuf, List)
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() < FramePacket.ALL_BITS) {
			return;
		}
		in.markReaderIndex();

		final byte[] header = new byte[FramePacket.ALL_BITS];
		in.readBytes(header);
		final long allLength = fromBytes(header, 0); //Integer.valueOf(new String(allLengthData));
//		LOGGER.info("all-length : {}，readableBytes：{}", allLength, in.readableBytes());
		if (in.readableBytes() < allLength - FramePacket.ALL_BITS) {
			in.resetReaderIndex();
			return;
		}
		final int propertiesLength = fromBytes(header, 4); //Integer.valueOf(new String(propertiesLengthData));
		final int seq = fromBytes(header, 8);
		final int typeCode = header[12] & 0xff;
//		LOGGER.info("properties-length : {} type-code: {}", propertiesLength, typeCode);
		final FrameType type = FrameType.valueOfCode(typeCode);
		if (type == FrameType.HEARTBEAT) {
			/* 心跳消息，不用进行后续处理，进入下一次读取 */
			return;
		}

		final byte[] propertiesData = new byte[propertiesLength];
		if (propertiesLength > 0) {
//			LOGGER.info("properties-length : {}，readableBytes：{}", propertiesData.length, in.readableBytes());
			in.readBytes(propertiesData);
		}
		final byte[] content = new byte[(int) (allLength - FramePacket.ALL_BITS - propertiesLength)];
		if (content.length > 0) {
//			LOGGER.info("content-length : {}，readableBytes：{}", content.length, in.readableBytes());
			in.readBytes(content);
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.info("content : {}", Arrays.toString(content));
		}
		final BMessage message = BMessage.create(propertiesData, content);
		message.setProperty(BProps.TYPE, type);
		out.add(new FramePacket(type, message));
	}

}
