package com.sitech.crmpd.idmm.broker.util;

/**
 *
 *
 * zk 里的数据结构:
 *
 * /(prefix)
 *  - ble
 *    -(ip):(port)   as ble_id
 *    -(ip):(port)
 *  - blecmd
 *    - (ip):(port)-(ble_id)
 *    - (ip):(port)-(ble_id)
 *  - partitions
 *    - (target_topic_id)
 *      - (client_id)
 *        - (part_id) (part_num):(part_status):(ble_id)
 *        - (part_id) (part_num):(part_status):(ble_id)
 *        - (part_id) (part_num):(part_status):(ble_id)
 *      - (client_id)
 *        ...
 *    - (target_topic_id)
 *      ...
 */

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;
import java.util.List;

@Configuration
public class ZK {
    private static final Logger log = LoggerFactory.getLogger(ZK.class);

    @Value("${zk.addr}")
    private String zk_addr;

    @Value("${zk.connectTimeout}")
    private int zk_connectTimeout;

    @Value("${zk.sessionTimeout}")
    private int zk_sessionTimeout;

    @Value("${zk.root}")
    private String prefix;

    private CuratorFramework zkClient;

    public void init() {
        RetryPolicy RETRYPOLICY = new RetryOneTime(10);
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString(zk_addr)
                .retryPolicy(RETRYPOLICY).connectionTimeoutMs(zk_connectTimeout)
                .sessionTimeoutMs(zk_sessionTimeout).build();

//        zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
//            @Override
//            public void stateChanged(CuratorFramework client, ConnectionState newState) {
//                if (ConnectionState.CONNECTED == newState) {
//
//                } else if (	ConnectionState.LOST == newState) {
//                    // 连接挂起或者丢失的情况下， 都直接关闭应用
//                    log.error("zookeeper connection lost, app exit...");
//                    System.exit(1);
//                } else if (ConnectionState.SUSPENDED == newState){
//                    log.warn("zookeeper connection suspended, waiting...");
//                }
//            }
//        });
        zkClient.start();
        try {
            zkClient.blockUntilConnected();
            log.info("zookeeper connected!");
        } catch (final InterruptedException e) {
            log.error("wait zookeeper connect failed, exit", e);
            System.exit(2);
        }
        this.zkClient = zkClient;
    }

    /**
     * 创建BLE启动的临时zk节点
     * @param mainAddr
     * @param cmdAddr
     */
    public String createBLE(String mainAddr, String cmdAddr) {
//        String localip = Util.getlocalip();
        mainAddr = mainAddr.substring(1);
        cmdAddr = cmdAddr.substring(1);
        String bleid = mainAddr;

        final String path_b = prefix + "/" + "ble" + "/" + bleid;
        final String path_c = prefix + "/" + "blecmd" + "/" + cmdAddr + "-" + bleid;
        try {
            zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(path_b, "".getBytes());
            log.info("bleid node register succ {}", path_b);
        } catch (Exception e) {
            log.error("create bleid node path[{}] failed", path_b, e);
            return null;
        }
        try {
            zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(path_c, "".getBytes());
            log.info("ble cmd port node register succ {}", path_c);
        } catch (Exception e) {
            log.error("ble cmd port {} node register failed", path_c, e);
            return null;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                try{
                    zkClient.delete().forPath(path_b);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                try{
                    zkClient.delete().forPath(path_c);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        return bleid;
    }

    public List<String[]> getBLEList()  {
        try {
            List<String[]> l = new LinkedList<>();
            for(String p: zkClient.getChildren().forPath(prefix + "/" + "blecmd") ){
                l.add(p.split("-"));
            }
            return l;
        } catch (Exception e) {
            log.error("get ble list failed", e);
            return null;
        }
    }

    public void createOneTopic(String topic, String client, int partCount, int partid){
        String basePath = prefix + "/" + topic + "/" +client;

        try {
            if(zkClient.checkExists().forPath(basePath) != null){
                //delete all children
                for(String c: zkClient.getChildren().forPath(basePath))
                    zkClient.delete().forPath(basePath + "/" + c);
            }else{
                zkClient.create().creatingParentsIfNeeded().forPath(basePath);
            }
            for(int i=0; i<partCount; i++) {
                //- (part_id):  (part_num)-(part_status)-(ble_id)
                String path = basePath + "/" + partid ++;
                zkClient.create().forPath(path, ((i+1)+":shut:none").getBytes());
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void close() {
        if(zkClient != null)
            zkClient.close();
    }

}
