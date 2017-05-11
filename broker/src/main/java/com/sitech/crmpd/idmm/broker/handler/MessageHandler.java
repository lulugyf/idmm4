package com.sitech.crmpd.idmm.broker.handler;

import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.MessageType;
import com.sitech.crmpd.idmm.client.exception.OperationException;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 能够根据消息内容自动应答的消息
 *
 * @author heihuwudi@gmail.com</br> Created By: 2015年3月31日 下午9:51:49
 */
public interface MessageHandler extends ApplicationContextAware {

	/**
	 * @return 输入消息类型
	 */
	MessageType getType();

	/**
	 * @return 输出消息类型
	 */
	MessageType getAnswerType();

	/**
	 * 处理消息
	 *
	 * @param context
	 *            连接上下文
	 * @param message
	 *            消息
	 * @return 返回消息
	 * @throws OperationException
	 */
	Message handle(ChannelHandlerContext context, Message message) throws OperationException;
}
