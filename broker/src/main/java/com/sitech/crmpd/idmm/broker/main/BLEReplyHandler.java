package com.sitech.crmpd.idmm.broker.main;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.broker.actor.BLEActor;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;


@ChannelHandler.Sharable
public class BLEReplyHandler extends SimpleChannelInboundHandler<FramePacket> {

    private static final Logger log = LoggerFactory.getLogger(BLEReplyHandler.class);

    private ActorRef ref;

    public BLEReplyHandler(ActorRef r){
        ref = r;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final FramePacket fm) throws Exception {
        ref.tell(new BLEActor.RMsg(ctx.channel(), fm), ActorRef.noSender());
//        log.info(fm.toString());
//        String ret = "done";
//        BMessage m = fm.getMessage();
//        if(fm.getType() == FrameType.BRK_PULL_ACK){
//            RetCode rcode = m.getEnumProperty(BProps.RESULT_CODE, RetCode.class);
//            log.info("{} {}", rcode == RetCode.OK, m.p(BProps.RESULT_CODE));
//            if(rcode == RetCode.OK){
//                ret = m.p(BProps.MESSAGE_ID);
//            }else{
//                ret = "";
//            }
//        }
//        wait.offer(ret);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
    }
}
