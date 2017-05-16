package com.sitech.crmpd.idmm.supervisor;

import com.alibaba.fastjson.JSON;
import com.sitech.crmpd.idmm.util.BZK;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.netapi.BMessage;
import com.sitech.crmpd.idmm.netapi.FrameCoder;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import com.sitech.crmpd.idmm.netapi.FrameType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * http://www.codeceo.com/article/netty-create-thread-pools.html
 * Created by guanyf on 2017/1/24.
 */
@Configuration
public class Supervisor implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);
    // 先实现向BLE分配主题分区

    // 集群唯一的分区id 数字累加， 需要把值存到zk中去， 并从中初始化
    private int partid_seed =  1;
    private int seq_seed = 1;

    @Resource
    private BZK zk;

    private ArrayBlockingQueue<FramePacket> wait = new ArrayBlockingQueue<>(100);
    private ReplyHandler handler = new ReplyHandler(wait);
    private EventLoopGroup group = new NioEventLoopGroup(5);
    private Bootstrap bootstrap;

    private List<BLEState> bles = new LinkedList<>();
    private Map<String, List<PartConfig>> parts = new HashMap<>(); //以 topic~client 为key的分区数据

    static class BLEState{
        Channel ch;
        String id;
        String cmdaddr;
        boolean isOk;

        int part_count;
    }

    public void startup(){
        new Thread(this).start();
    }

    public void run() {

        try{
            init();

            while(true){
                if(zk.becomeSupervisor("supervisor"))
                    break;
                else
                    Thread.sleep(5000L);
            }

            log.info("starting supvisor");

            getList();

            zk.watchBLEChange(new BZK.CallBack() {
                @Override
                public void call() {
                    getList();
                }
            });

            query();

            // for test only
            startTopic("topic", "client", 20, 10);

        }catch(Exception ex){
            log.error("init failed", ex);
        }
    }

    // 查询分区数据
    private void query() {
        log.info("query -----");
        parts.clear();
        for(BLEState b: bles){
            FramePacket f = new FramePacket(FrameType.CMD_PT_QUERY,
                    BMessage.c(), seq_seed++ );
            b.ch.writeAndFlush(f);

            try {
                FramePacket fr = wait.take();
                String body = fr.getMessage().getContentAsString();
                log.debug("query reply from BLE: {}", body);

                List<PartConfig> l = JSON.parseArray(body, PartConfig.class);
                for(PartConfig p: l){
                    String k = p.getTopicId() + "~" + p.getClientId();
                    List<PartConfig> pl = parts.getOrDefault(k, new LinkedList<>());
                    if(pl.size() == 0)
                        parts.put(k, pl);
                    pl.add(p);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void startTopic(String topic, String client, int maxOnway, int partCount) {
        // TODO sort the ble list

        String k = topic + "~" + client;
        if(parts.containsKey(k)){
            log.info("topic {} already started", k);
            return;
        }

        // and start partitions
        int partid = partid_seed;
        partid_seed += partCount;
        zk.createOneTopic(topic, client, partCount, partid);

        for(int i=0; i<partCount; i++) {
            BLEState b = bles.get(i % bles.size());

            PartConfig p = new PartConfig();
            p.setClientId(client);
            p.setTopicId(topic);
            p.setMaxOnWay(maxOnway);
            p.setPartId(partid ++);
            p.setPartNum(i+1);
            p.setStatus(PartitionStatus.READY);
            FramePacket f = new FramePacket(FrameType.CMD_PT_START,
                    BMessage.create(JSON.toJSONString(p)), seq_seed++ );
            b.ch.writeAndFlush(f);

            try {
                log.info("reply from BLE: {}", wait.take().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        zk.setMaxPartid(partid_seed);
    }

    /**
     * 与BLE的cmd端口建立连接
     */
    private void getList() {
//        bles.clear();

        for(String[] x: zk.getBLEList()) {
            String bleid = x[1];
            boolean found = false;
            for(BLEState y: bles){
                if(bleid.equals(y.id)){
                    found = true;
                    break;
                }
            }
            if(found) continue;
            BLEState b = new BLEState();
            b.id = x[1];
            b.cmdaddr = x[0];
            try{
                String v = b.cmdaddr;
                int p = v.indexOf(':');
                b.ch = bootstrap.connect(v.substring(0, p), Integer.parseInt(v.substring(p+1))).sync().channel();
                b.isOk = true;
                bles.add(b);
            }catch(Exception ex){
                log.error("", ex);
            }
        }
    }

    private void init() throws  Exception {
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
        bootstrap = b;

        partid_seed = zk.getMaxPartid();

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
//            Channel ch = b.connect("127.0.0.1", 7172).sync().channel();
//
//            PartConfig p = new PartConfig();
//            p.setClientId("clientid");
//            p.setTopicId("topic");
//            p.setMaxOnWay(10);
//            p.setPartId(12312312);
//            p.setPartNum(1);
//            p.setStatus(PartitionStatus.READY);
//            FramePacket f = new FramePacket(FrameType.CMD_PT_START,
//                    BMessage.create(p.toString()) );
//            ch.writeAndFlush(f);
//            wait.take();
//
//            ch.close();
//
//            ch.closeFuture().sync();
//        }finally {
//            group.shutdownGracefully();
//        }

    }



}
