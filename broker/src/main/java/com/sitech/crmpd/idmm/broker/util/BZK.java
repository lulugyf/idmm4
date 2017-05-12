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

import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
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
public class BZK {
    private static final Logger log = LoggerFactory.getLogger(BZK.class);

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
        String basePath = prefix + "/partitions/" + topic + "/" +client;

        try {
            if(zkClient.checkExists().forPath(basePath) != null){
                //delete all children
                for(String c: zkClient.getChildren().forPath(basePath))
                    zkClient.delete().forPath(basePath + "/" + c);
            }else{
                zkClient.create().creatingParentsIfNeeded().forPath(basePath);
            }
            for(int i=0; i<partCount; i++) {
                //- (part_id):  (part_num)~(part_status)~(ble_id)
                String path = basePath + "/" + partid ++;
                zkClient.create().forPath(path,
                        ((i+1)+"~"+ PartitionStatus.SHUT.name()+"~none").getBytes());
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public int getMaxPartid() {
        String path = prefix + "/partitions/maxpartid";
        try {
            if(zkClient.checkExists().forPath(path) == null){
                return 1;
            }else{
                int r = Integer.parseInt(new String(zkClient.getData().forPath(path) ))+1;
                if(r > (Math.pow(2, 30))){
                    r = 1;
                }
                return r;
            }
        } catch (Exception e) {
            log.error("", e);
            return 1;
        }
    }
    public void setMaxPartid(int partid) {
        String path = prefix + "/partitions/maxpartid";
        try {
            if(zkClient.checkExists().forPath(path) == null){
                zkClient.create().forPath(path, String.valueOf(partid).getBytes());
            }else{
                zkClient.setData().forPath(path, String.valueOf(partid).getBytes());
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 列出一个分区的全部数据
     * @param topic
     * @param client
     * @return
     */
    public List<PartConfig> getParts(String topic, String client) {
        String basePath = prefix + "/partitions/" + topic + "/" +client;
        try {
            if(zkClient.checkExists().forPath(basePath) == null){
                return null;
            }
            List<PartConfig> r = new LinkedList<>();
            PartConfig c1 = new PartConfig();
            c1.setTopicId(topic);
            c1.setClientId(client);
            for(String partid: zkClient.getChildren().forPath(basePath)) {
                //- (part_id):  (part_num)~(part_status)~(ble_id)
                String path = basePath + "/" + partid;
                String data = new String(zkClient.getData().forPath(path));
                PartConfig c = c1.clone();
                c.setPartId(Integer.parseInt(partid));
                String[] d = data.split("~");
                c.setPartNum(Integer.parseInt(d[0]));
                c.setStatus(PartitionStatus.valueOf(d[1]));
                c.setBleid(d[2]);
                r.add(c);
            }
            return r;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    public void close() {
        if(zkClient != null)
            zkClient.close();
    }

}
