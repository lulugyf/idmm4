package com.sitech.crmpd.idmm.broker.main;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.sitech.crmpd.idmm.broker.actor.BLEActor;
import com.sitech.crmpd.idmm.broker.actor.PersistActor;
import com.sitech.crmpd.idmm.broker.actor.ReplyActor;
import com.sitech.crmpd.idmm.broker.config.TopicMapping;
import com.sitech.crmpd.idmm.broker.handler.LogicHandler;
import com.sitech.crmpd.idmm.broker.util.BZK;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.supervisor.Supervisor;
import com.sitech.crmpd.idmm.transport.FrameCodeC;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Resource;

@Configuration
public class BrokerServer {
    private static final Logger log = LoggerFactory.getLogger(BrokerServer.class);

    @Value("${netty.bossCount:1}")
    private int nt_bossCount;

    @Value("${netty.workerCount:10}")
    private int nt_workerCount;

    @Value("${netty.local.addr}")
    private String nt_local_addr;

    @Value("${actor.replyCount:20}")
    private int replyCount; //tcp 应答actor数量
    @Value("${actor.persistentCount:30}")
    private int persistentCount; //持久化actor数量

    @Resource
    private LogicHandler logicHandler;

    @Resource
    private BZK zk;
    private String bleid;

    @Resource
    private Supervisor spv;

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = null;
        try {
            applicationContext = new ClassPathXmlApplicationContext("application.xml");

            BrokerServer server = applicationContext.getBean(BrokerServer.class);
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

    @Bean
    public Cache messageCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Message.class,
                                ResourcePoolsBuilder.heap(100))
                                .build())
                .build(true);

        Cache<String, Message> preConfigured
                = cacheManager.getCache("preConfigured", String.class, Message.class);
        return preConfigured;

//        Cache<Long, String> myCache = cacheManager.createCache("myCache",
//                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
//                        ResourcePoolsBuilder.heap(100)).build());
//
//        myCache.put(1L, "da one!");
//        String value = myCache.get(1L);
//
//        cacheManager.close();
    }

    public void startup() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(nt_bossCount);
        EventLoopGroup workerGroup = new NioEventLoopGroup(nt_workerCount);

//        final EventLoopGroup eventExceuteGroup = new NioEventLoopGroup(10);
//        final ExecutorService executorService =
//                MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(50));

        ActorSystem system = ActorSystem.create("root");
        ActorRef replyActor = system.actorOf(Props.create(ReplyActor.class, replyCount), "creply");
        ActorRef persistActor = system.actorOf(Props.create(PersistActor.class, persistentCount, replyActor), "persist");
        ActorRef bleActor = system.actorOf(Props.create(BLEActor.class), "ble");

        logicHandler.setRef("creply", replyActor);
        logicHandler.setRef("persist", persistActor);
        logicHandler.setRef("ble", bleActor);

//        // 逐个提供 ActorRef
//        cmdactor.tell(new RefMsg("store", storeActor), system.deadLetters());
//        cmdactor.tell(new RefMsg("brk", brkactor), system.deadLetters());
//        cmdactor.tell(new RefMsg("reply", replyActor), system.deadLetters());
//        brkactor.tell(new RefMsg("cmd", cmdactor), system.deadLetters());
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
                            pipeline.addLast(new FrameCodeC());
                            pipeline.addLast(logicHandler);
                        }
                    });


            int port1 = Integer.parseInt(nt_local_addr.substring(nt_local_addr.indexOf(':')+1));
            String brk_host = nt_local_addr.substring(0, nt_local_addr.indexOf(':'));
            Channel ch = bootstrap.bind(brk_host, port1).sync().channel();
            ChannelFuture cf = ch.closeFuture();

            zk.init();
            spv.startup(); // 启动supervisor

            cf.sync();
        } finally {
            system.terminate();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
