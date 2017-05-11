package com.sitech.crmpd.idmm.ble.main;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.ble.actor.CmdActor;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CmdServerHandler extends SimpleChannelInboundHandler<FramePacket> {

    private static final Logger logger = LoggerFactory.getLogger(CmdServerHandler.class);

    private ActorRef actor;

    public CmdServerHandler(ActorRef a){
        this.actor = a;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FramePacket framemessage) throws Exception {
        actor.tell(new CmdActor.Msg(ctx.channel(), framemessage), ActorRef.noSender());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("cmd channel active:{}", ctx.channel().remoteAddress());
    }
}
