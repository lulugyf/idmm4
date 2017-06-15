package com.sitech.crmpd.idmm.supervisor;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;


@ChannelHandler.Sharable
public class ReplyHandler extends SimpleChannelInboundHandler<FramePacket> {

    private static final Logger log = LoggerFactory.getLogger(ReplyHandler.class);
    private ActorRef ref;

    public ReplyHandler(ActorRef ref){
        this.ref = ref;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final FramePacket fm) throws Exception {
//        logger.info(fm.toString());
        ref.tell(new SupActor.Msg(ctx.channel(), fm), ActorRef.noSender());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
        log.error("connection exception {}", ctx.channel().remoteAddress(), cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.warn("{} connected", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.warn("{} disconnected", ctx.channel().remoteAddress());
    }
}
