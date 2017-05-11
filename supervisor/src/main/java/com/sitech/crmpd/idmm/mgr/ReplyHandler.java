package com.sitech.crmpd.idmm.mgr;

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

    private ArrayBlockingQueue<String> wait;

    public ReplyHandler(Object[] param){
        this.wait = (ArrayBlockingQueue<String>)param[0];
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final FramePacket fm) throws Exception {
        logger.info(fm.toString());
        wait.offer("done");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
    }
}
