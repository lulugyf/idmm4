package com.sitech.crmpd.idmm.mgr;

import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * http://www.codeceo.com/article/netty-create-thread-pools.html
 * Created by guanyf on 2017/1/24.
 */
public class MgrServer {
    // 先实现向BLE分配主题分区
    private AtomicInteger partid_seed;
    public static void main(String[] args) throws Exception {

        ArrayBlockingQueue<String> wait = new ArrayBlockingQueue<String>(10);
        EventLoopGroup group = new NioEventLoopGroup();


        final ReplyHandler handler =
                new ReplyHandler(new Object[]{wait});

        try {

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(new FrameCoder());
                            p.addLast(handler);
                        }
                    });

            Channel ch = b.connect("127.0.0.1", 7172).sync().channel();

            PartConfig p = new PartConfig();
            p.setClientId("clientid");
            p.setTopicId("topic");
            p.setMaxOnWay(10);
            p.setPartId(12312312);
            p.setPartNum(1);
            p.setStatus(PartitionStatus.READY);
            FramePacket f = new FramePacket(FrameType.CMD_PT_START,
                    BMessage.create(p.toString()) );
            ch.writeAndFlush(f);
            wait.take();

            ch.close();

            ch.closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }

    }



}
