package com.sitech.crmpd.idmm.broker.main;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;


@Configuration
public class BLEClient {
    private static final Logger log = LoggerFactory.getLogger(BLEClient.class);

    @Value("${ble.client.threads:5}")
    private int bleclientThreads;

    private Bootstrap bootstrap;
    private ActorRef ble;
    private EventLoopGroup group;
    private HashMap<String, ChannelState> chs = new HashMap<>();

    private static class ChannelState{
        Channel ch;
        long refresh_time;
        ChannelState(Channel c) {
            ch = c;
            refresh_time = System.currentTimeMillis();
        }
    }

    public void init(ActorRef ble)
    {
        group = new NioEventLoopGroup(bleclientThreads);
        this.ble = ble;

        final BLEReplyHandler handler =
                new BLEReplyHandler(ble);

        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(new FrameCoder());
                            p.addLast(handler);
                        }
                    });
        }catch(Exception ex){
            log.error("create bootstrap for ble failed", ex);
            group.shutdownGracefully();
        }
    }

    /**
     * 获取到BLE的连接, 非线程安全
     * @param addr
     * @return
     */
    public Channel getCh(String addr) {
        if(chs.containsKey(addr))
            return chs.get(addr).ch;
        else{
            int p = addr.indexOf(':');
            String host = addr.substring(0, p);
            int port = Integer.parseInt(addr.substring(p+1));
            try {
                // TODO 同步的连接可能阻塞唯一的BLEActor, 需要考虑避免同步
                Channel ch = bootstrap.connect(host, port).sync().channel();
                chs.put(addr, new ChannelState(ch));
                log.info("BLE connection to {} created", addr);
                return ch;
            }catch(Exception ex){
                log.error("connect to BLE {} failed", addr, ex);
                return null;
            }
        }
    }

    public void close() {
        if(group != null) {
            group.shutdownGracefully();
            group = null;
        }
    }
//    public static void main(String[] args) throws Exception {
//
//        ArrayBlockingQueue<String> wait = new ArrayBlockingQueue<String>(10);
//        EventLoopGroup group = new NioEventLoopGroup();
//
//
//        final BLEReplyHandler handler =
//                new BLEReplyHandler(new Object[]{wait});
//
//        try {
//
//            Bootstrap b = new Bootstrap();
//            b.group(group)
//                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.TCP_NODELAY, true)
//                    .handler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel ch) {
//                            ChannelPipeline p = ch.pipeline();
//
//                            p.addLast(new FrameCoder());
//                            p.addLast(handler);
//                        }
//                    });
//
//            Channel ch = b.connect("127.0.0.1", 7171).sync().channel();
//
//            String ret = null;
//            int seq = 8172;
//            FramePacket f = new FramePacket(FrameType.BRK_SEND_COMMIT,
//                    BMessage.c().p(BProps.MESSAGE_ID, "881239")
//                            .p(BProps.GROUP, "abc")
//                            .p(BProps.PRIORITY, 123)
//                    .p(BProps.PART_ID, 12312312), seq );
//            ch.writeAndFlush(f);
//            wait.take();
//
//            seq ++;
//            ch.writeAndFlush(new FramePacket(FrameType.BRK_PULL,
//                    BMessage.c().p(BProps.PART_ID, 12312312)
//                    .p(BProps.PROCESSING_TIME, 60), seq )
//            );
//            ret = wait.take();
//            System.out.println("------msgid: "+ret);
//
//            if(!"".equals(ret)) {
//                seq++;
//                ch.writeAndFlush(new FramePacket(FrameType.BRK_COMMIT,
//                        BMessage.c().p(BProps.PART_ID, 12312312)
//                        .p(BProps.MESSAGE_ID, ret), seq)
//                );
//                wait.take();
//            }
//
//            ch.close();
//
//            ch.closeFuture().sync();
//        }finally {
//            group.shutdownGracefully();
//        }
//
//    }



}
