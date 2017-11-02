package com.sitech.crmpd.idmm.ble.main;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.codahale.metrics.MetricRegistry;
import com.sitech.crmpd.idmm.ble.actor.*;
import com.sitech.crmpd.idmm.netapi.FrameCoder;
import com.sitech.crmpd.idmm.util.Mon;
import com.sitech.crmpd.idmm.util.Util;
import com.sitech.crmpd.idmm.util.ZK;
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
import static com.sitech.crmpd.idmm.ble.main.spring.SpringExtension.SPRING_EXTENSION_PROVIDER;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

@Configuration
public class BLEServer {
    private static final Logger log = LoggerFactory.getLogger(BLEServer.class);

    @Value("${netty.bossCount:3}")
    private int nt_bossCount;

    @Value("${netty.workerCount:10}")
    private int nt_workerCount;

    @Resource
    private Mon mon;

//    @Value("${netty.brk.addr}")
//    private String nt_brk_addr;
//    @Value("${netty.cmd.addr}")
//    private String nt_cmd_addr;

    @Resource
    private ZK zk;

    @Resource
    private ActorSystem system;

    @Resource
    private MetricRegistry metrics;


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

    static class ServicePort{
        private SimpleChannelInboundHandler handler;
        private String name;
        private int port;
        private Channel channel;
        private ChannelFuture cf;
        ServicePort(String name, SimpleChannelInboundHandler h, int port){
            this.name = name;
            this.port = port;
            this.handler = h;
        }
    }

    public void startup() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(nt_bossCount);
        EventLoopGroup workerGroup = new NioEventLoopGroup(nt_workerCount);


//        final EventLoopGroup eventExceuteGroup = new NioEventLoopGroup(10);
//        final ExecutorService executorService =
//                MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(50));

//        ActorSystem system = ActorSystem.create("root");
        ActorRef replyActor = getRef(ReplyActor.class, "reply");
        ActorRef storeActor = getRef(PersistentActor.class, "persist");
        ActorRef cmdactor = getRef(CmdActor.class, "cmd");
        ActorRef brkactor = getRef(BrkActor.class, "brk");

        // 逐个提供 ActorRef
        cmdactor.tell(new RefMsg("persist", storeActor), ActorRef.noSender());
        cmdactor.tell(new RefMsg("brk", brkactor), ActorRef.noSender());
        cmdactor.tell(new RefMsg("reply", replyActor), ActorRef.noSender());
//        cmdactor.tell(new RefMsg("metrics", null, metrics), ActorRef.noSender());
        brkactor.tell(new RefMsg("cmd", cmdactor), ActorRef.noSender());
        //brkactor.tell(new RefMsg("reply", replyActor), system.deadLetters());

        List<ServicePort> plist = new LinkedList<>();
        plist.add(new ServicePort("dataport", new BrkServerHandler(brkactor), 0));
        plist.add(new ServicePort("cmdport", new CmdServerHandler(cmdactor), 0));

        try {
            for(ServicePort sp: plist) {
                log.info("init listen port {} on {}", sp.name, sp.port);
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new FrameCoder());
                                pipeline.addLast(sp.handler);
                            }
                        });

                Channel ch = bootstrap.bind("0.0.0.0", sp.port).sync().channel();
                if(sp.port == 0){
                    int port1 = ((InetSocketAddress) ch.localAddress()).getPort();
                    sp.port = port1;
                    log.info("bind port on {}", port1);
                }
                ChannelFuture cf = ch.closeFuture();
                sp.channel = ch;
                sp.cf = cf;
            }

            zk.init();

            String hostAddr = Util.getMyAddr(zk.getZkAddr());

            // 把zk传送给CmdActor
            cmdactor.tell(new RefMsg("zk", null, zk), ActorRef.noSender());

            int port1 = plist.get(0).port, port2 = plist.get(1).port;
            bleid = zk.createBLE(hostAddr+":"+port1,
                    hostAddr+":"+port2);

//            test();
            if(bleid != null) {
                mon.setNodeid("bl-"+bleid);
                log.warn("startup successfully!");

                for(ServicePort sp: plist)
                    sp.cf.sync();

            }else{
                log.error("startup failed!");
                for(ServicePort sp: plist)
                    sp.channel.close();

                system.terminate();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    private ActorRef getRef(Class<? extends Actor> actorClass, String name) {
        return system.actorOf(SPRING_EXTENSION_PROVIDER.get(system)
                .props(actorClass), name);
    }

    private void test() {
        ActorRef test1 = system.actorOf(SPRING_EXTENSION_PROVIDER.get(system)
                .props(TestActor.class, "aa"), "actorname");
        test1.tell("hello", ActorRef.noSender());

        ActorRef test2 = system.actorOf(SPRING_EXTENSION_PROVIDER.get(system)
                .props(TestActor.class, "bb"), "actorname1");
        test2.tell("hellobb", ActorRef.noSender());
    }
}
