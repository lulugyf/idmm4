package com.sitech.crmpd.idmm.supervisor;

import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;


@ChannelHandler.Sharable
public class ReplyHandler extends SimpleChannelInboundHandler<FramePacket> {

    private static final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);

    private ArrayBlockingQueue<FramePacket> wait;

    public ReplyHandler(ArrayBlockingQueue<FramePacket> w){
        this.wait = w;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final FramePacket fm) throws Exception {
//        logger.info(fm.toString());
        wait.offer(fm);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
    }
}
