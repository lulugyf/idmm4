package com.sitech.crmpd.idmm.supervisor;

import com.sitech.crmpd.idmm.netapi.BMessage;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import com.sitech.crmpd.idmm.netapi.FrameType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by guanyf on 6/27/2017.
 */

@ChannelHandler.Sharable
public class IdleHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(IdleHandler.class);
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                log.info("write a heartbeat to {}", ctx.channel().remoteAddress());
                ctx.channel().writeAndFlush(new FramePacket(FrameType.HEARTBEAT, BMessage.c(), 0));
            }
        }
    }
}
