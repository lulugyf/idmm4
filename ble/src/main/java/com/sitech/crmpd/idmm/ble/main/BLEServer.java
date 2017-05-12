package com.sitech.crmpd.idmm.ble.main;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.sitech.crmpd.idmm.ble.actor.*;
import com.sitech.crmpd.idmm.ble.util.SZK;
import com.sitech.crmpd.idmm.netapi.FrameCoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Resource;

@Configuration
public class BLEServer {
    private static final Logger log = LoggerFactory.getLogger(BLEServer.class);

    @Value("${netty.bossCount:1}")
    private int nt_bossCount;

    @Value("${netty.workerCount:10}")
    private int nt_workerCount;

    @Value("${netty.brk.addr}")
    private String nt_brk_addr;
    @Value("${netty.cmd.addr}")
    private String nt_cmd_addr;

    @Value("${actor.replyCount:20}")
    private int replyCount; //tcp 应答actor数量
    @Value("${actor.persistentCount:30}")
    private int persistentCount; //持久化actor数量


    @Resource
    private SZK zk;
    private String bleid;

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = null;
        try {
            applicationContext = new ClassPathXmlApplicationContext("application.xml");

            BLEServer server = applicationContext.getBean(BLEServer.class);
            server.startup();


        } catch (final Exception e) {
            log.error("startup failed", e);
        } finally {
            if(applicationContext != null) {
                applicationContext.close();
                log.error("spring exit");
            }
        }

    }

    public void startup() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(nt_bossCount);
        EventLoopGroup workerGroup = new NioEventLoopGroup(nt_workerCount);

//        final EventLoopGroup eventExceuteGroup = new NioEventLoopGroup(10);
//        final ExecutorService executorService =
//                MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(50));

        ActorSystem system = ActorSystem.create("root");
        ActorRef replyActor = system.actorOf(Props.create(ReplyActor.class, replyCount), "reply");
        ActorRef storeActor = system.actorOf(Props.create(PersistentActor.class, persistentCount), "store");
        ActorRef cmdactor = system.actorOf(Props.create(CmdActor.class), "cmd");
        ActorRef brkactor = system.actorOf(Props.create(BrkActor.class), "brk");

        // 逐个提供 ActorRef
        cmdactor.tell(new RefMsg("store", storeActor), ActorRef.noSender());
        cmdactor.tell(new RefMsg("brk", brkactor), ActorRef.noSender());
        cmdactor.tell(new RefMsg("reply", replyActor), ActorRef.noSender());
        brkactor.tell(new RefMsg("cmd", cmdactor), ActorRef.noSender());
        //brkactor.tell(new RefMsg("reply", replyActor), system.deadLetters());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new FrameCoder());
                            pipeline.addLast(new BrkServerHandler(brkactor));
                        }
                    });


            int port1 = Integer.parseInt(nt_brk_addr.substring(nt_brk_addr.indexOf(':')+1));
            String brk_host = nt_brk_addr.substring(0, nt_brk_addr.indexOf(':'));
            Channel ch = bootstrap.bind(brk_host, port1).sync().channel();
            ChannelFuture cf = ch.closeFuture();

            ServerBootstrap bootstrap1 = new ServerBootstrap();
            bootstrap1.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new FrameCoder());
                            pipeline.addLast(new CmdServerHandler(cmdactor));
                        }
                    });

            int port2 = Integer.parseInt(nt_cmd_addr.substring(nt_cmd_addr.indexOf(':')+1));
            String cmd_host = nt_cmd_addr.substring(0, nt_cmd_addr.indexOf(':'));
            Channel ch1 = bootstrap1.bind(cmd_host, port2).sync().channel();
            ChannelFuture cf1 = ch1.closeFuture();

            zk.init();

            // 把zk传送给CmdActor
            cmdactor.tell(new RefMsg("zk", null, zk), ActorRef.noSender());

            bleid = zk.createBLE(ch.localAddress().toString(), ch1.localAddress().toString());
            if(bleid != null) {
                log.warn("startup successfully!");

                cf.sync();
                cf1.sync();
            }else{
                log.error("startup failed!");

                ch.close();
                ch1.close();

                system.terminate();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
