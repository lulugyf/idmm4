package com.sitech.crmpd.idmm.broker.main;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.sitech.crmpd.idmm.broker.actor.*;
import com.sitech.crmpd.idmm.broker.config.Config;
import com.sitech.crmpd.idmm.broker.config.ConsumeParts;
import com.sitech.crmpd.idmm.broker.config.Parts;
import com.sitech.crmpd.idmm.broker.handler.LogicHandler;
import com.sitech.crmpd.idmm.util.BZK;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Resource
    private BLEClient bleclient;

    private ActorRef bleActor;

    private Map<String, ActorRef> bles = new HashMap<>();

    private CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .build(true);

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
    public Cache<String, Message> messageCache() {
//        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
//                .withCache("preConfigured",
//                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Message.class,
//                                ResourcePoolsBuilder.heap(100))
//                                .build())
//                .build(true);
//
//        Cache<String, Message> cache
//                = cacheManager.getCache("preConfigured", String.class, Message.class);

        Cache<String, Message> cache = cacheManager.createCache("messageCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Message.class,
                        ResourcePoolsBuilder.heap(100)).build() );

        return cache;

//        Cache<Long, String> myCache = cacheManager.createCache("myCache",
//                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
//                        ResourcePoolsBuilder.heap(100)).build());
//
//        myCache.put(1L, "da one!");
//        String value = myCache.get(1L);
//
//        cacheManager.close();
    }

    public Cache<String, Message> persistCache() {
        Cache<String, Message> cache = cacheManager.createCache("persistCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Message.class,
                        ResourcePoolsBuilder.heap(100)).build() );

        return cache;
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
        this.bleActor = bleActor;

        logicHandler.setRef("creply", replyActor);
        logicHandler.setRef("persist", persistActor);
        logicHandler.setRef("ble", bleActor);

//        // 逐个提供 ActorRef
//        cmdactor.tell(new RefMsg("store", storeActor), system.deadLetters());
//        cmdactor.tell(new RefMsg("brk", brkactor), system.deadLetters());
        bleActor.tell(new RefMsg("reply", replyActor), ActorRef.noSender());
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
            spv.startup(); // 尝试启动supervisor, 通过zk竞争

            //TODO 获取BLE列表, 并添加监视更改的功能
            bleclient.init(bleActor);

            partsChanged();

            // TODO 已经添加了分区变化的更新监视, 但需要做延迟处理, 一个避免过于频繁的更新, 另一个避免漏掉更新
            zk.watchPartChange(new BZK.CallBack() {
                @Override
                public void call() {
                    isPartsChanged = true;
                    partsChanged();
                }
            });

            // 两个配置数据的设置: 主题映射和订阅关系
            logicHandler.setTopicMapping(Config.getMap());
            logicHandler.setSubscribes(Config.getSub());

            refreshBLEList(bleActor, replyActor, system);
            zk.watchBLEChange(new BZK.CallBack() {
                @Override
                public void call() {
                    refreshBLEList(bleActor, replyActor, system);
                }
            });

            zk.createBroker(nt_local_addr); //向zk注册broker地址

            cf.sync(); //阻塞调用
        } finally {
            system.terminate();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private volatile boolean isPartsChanged = true;
    private void partsChanged() {
        if(isPartsChanged){
            isPartsChanged = false;
        }else{
            return;
        }
        Parts parts = new Parts();
        ConsumeParts cp = new ConsumeParts();
        parts.setAllParts(zk, cp);
        logicHandler.setParts(parts);
        bleActor.tell(cp, ActorRef.noSender());
        log.warn("part status changed");
    }

    /**
     * 与BLE的通讯端口建立连接
     */
    private void refreshBLEList(ActorRef ble, ActorRef reply, ActorSystem system) {
        for(String[] v: zk.getBLEList()){
            String bleid = v[1];
            if(bles.containsKey(bleid)){

            }else{
                Channel ch = bleclient.getCh(bleid);
                ActorRef ref = system.actorOf(Props.create(BLEWriterActor.class, ch, reply), bleid);
                ble.tell(new RefMsg("ble", ref, bleid, ch), ActorRef.noSender());
                bles.put(bleid, ref);
            }
        }
    }
}
