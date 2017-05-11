package com.sitech.crmpd.idmm.broker.main;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.ble.actor.BrkActor;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 接收从broker发送过来的消息
 * 功能是 向不同的分区处理actor分发请求消息
 */

public class BrkServerHandler extends SimpleChannelInboundHandler<FramePacket> {

    private static final Logger logger = LoggerFactory.getLogger(BrkServerHandler.class);

    private ActorRef actor;

    public BrkServerHandler(ActorRef a){
        actor = a;
    }




    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FramePacket framemessage) throws Exception {
        actor.tell(new BrkActor.Msg(ctx.channel(), framemessage), ActorRef.noSender());

        /*
        ListenableFuture<FramePacket> listenableFuture =
                (ListenableFuture<FramePacket>)executorService.submit(new Callable<FramePacket>() {
            public FramePacket call() throws Exception{
                FrameType msgtype = framemessage.getType();
                BMessage message = framemessage.getMessage();
//                System.out.println(message.getPropertiesAsString());
//                System.out.println(message.getContentAsString());
                int priority = message.getIntegerProperty(BProps.PRIORITY);
//                System.out.println(priority);

                BMessage answerMessage = BMessage.create("hello world from ble!".getBytes());
                answerMessage.setProperty(BProps.MESSAGE_ID, message.getId());
                FramePacket answerFrameMessage = new FramePacket(FrameType.ANSWER, answerMessage);
                return answerFrameMessage;
            }
        });

        Futures.addCallback(listenableFuture, new FutureCallback<FramePacket>() {
            @Override
            public void onSuccess(FramePacket frameMessage) {
                ctx.writeAndFlush(frameMessage);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        }, executorService);

//        FrameType msgtype = framemessage.getType();
//        BMessage message = framemessage.getMessage();
////        System.out.println(message.getPropertiesAsString());
////        System.out.println(message.getContentAsString());
//        int priority = message.getIntegerProperty(BProps.PRIORITY);
////        System.out.println(priority);
//
//        BMessage answerMessage = BMessage.create("hello world from ble!".getBytes());
//        answerMessage.setProperty(BProps.MESSAGE_ID, message.getId());
//        FramePacket answerFrameMessage = new FramePacket(FrameType.ANSWER, answerMessage);
//        ctx.writeAndFlush(answerFrameMessage);
*/

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("channel active:{}", ctx.channel().remoteAddress());
    }
}
