package com.sitech.crmpd.idmm.util;

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
import com.sitech.crmpd.idmm.cfg.PartStatus;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
//import org.apache.curator.framework.api.transaction.CuratorOp;
//import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private final static String partChg = "partitions_change";

    private CuratorFramework zkClient;
    private String bleid;

    public static interface CallBack {
        public void call();
    }

    public String getZkAddr() {
        return zk_addr;
    }

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
//        mainAddr = mainAddr.substring(1);
//        cmdAddr = cmdAddr.substring(1);
        String bleid = mainAddr;

        final String path_b = prefix + "/" + "ble" + "/" + bleid;
        try {
            if(zkClient.checkExists().forPath(path_b) == null){
                zkClient.create().creatingParentsIfNeeded()
                        //.withMode(CreateMode.EPHEMERAL)
                        .forPath(path_b, cmdAddr.getBytes());
            }else{
                zkClient.setData()
                        //.withMode(CreateMode.EPHEMERAL)
                        .forPath(path_b, cmdAddr.getBytes());
            }

            log.info("bleid node register succ {}", path_b);
        } catch (Exception e) {
            log.error("create bleid node path[{}] failed", path_b, e);
            return null;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
            try{
                zkClient.delete().forPath(path_b);
            }catch(Exception ex){
                ex.printStackTrace();
            }
            }
        });
        this.bleid = bleid;
        return bleid;
    }

    public void removeBLE(String bleid) {
        final String path_b = prefix + "/" + "ble" + "/" + bleid;
        try {
            if(zkClient.checkExists().forPath(path_b) != null){
                zkClient.delete().forPath(path_b);
            }
            log.info("bleid node removed {}", path_b);
        } catch (Exception e) {
            log.error("create bleid node path[{}] failed", path_b, e);
        }

    }

    public void createBroker(String addr) {
        final String path = prefix + "/" + "broker" + "/" + addr;
        try {
            zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(path, "".getBytes());
            log.info("broker port node register succ {}", path);
        } catch (Exception e) {
            log.error("ble cmd port {} node register failed", path, e);
        }
    }

    /**
     * 更新分区状态, 并更新 part_change, 以便触发watcher
     * @param p
     */
    public void chgPartStatus(PartConfig p) {
//            String topic, String client, int partnum, int partid, PartStatus status){
        String path = prefix + "/partitions/" + p.getQid() +"/" + p.getPartId();

        try {
            //- (part_id):  (part_num)~(part_status)~(ble_id)
            zkClient.setData().forPath(path,
                    (p.getPartNum()+"~"+ p.getStatus().name()+"~"+bleid).getBytes());

            // 修改 parts 数据变动的路径, 键值采用当前时间
            path = prefix + "/" + "part_change";
            if(zkClient.checkExists().forPath(path) == null){
                zkClient.create().forPath(path,
                        String.valueOf(System.currentTimeMillis()).getBytes());
            }else{
                zkClient.setData().forPath(path,
                        String.valueOf(System.currentTimeMillis()).getBytes());
            }
        } catch (Exception e) {
            log.error("", e);
        }

    }

    public Map<String, String> getBLEList()  {
        String basePath = prefix + "/" + "ble";
        try {
            Map<String, String> l = new HashMap<>();
            for(String bleid: zkClient.getChildren().forPath(basePath) ){
                String cmdAddr = new String(zkClient.getData().forPath(basePath + "/" + bleid) );
                l.put(bleid, cmdAddr);
            }
            return l;
        } catch (Exception e) {
            log.error("get ble list failed", e);
            return null;
        }
    }

//    public void createInitialQueue(String qid, int partCount, int partid){
//        String basePath = prefix + "/partitions/" + qid;
//
//        try {
////            if(zkClient.checkExists().forPath(basePath) != null){
////                //delete all children
////                for(String c: zkClient.getChildren().forPath(basePath))
////                    zkClient.delete().forPath(basePath + "/" + c);
////            }else{
//                zkClient.create().creatingParentsIfNeeded().forPath(basePath);
////            }
//            List<CuratorOp> ops = new LinkedList<>();
//            for(int i=0; i<partCount; i++) {
//                //- (part_id):  (part_num)~(part_status)~(ble_id)
//                String path = basePath + "/" + partid ++;
//                ops.add(zkClient.transactionOp().create().forPath(path,
//                        ((i+1)+"~"+ PartStatus.SHUT.name()+"~none").getBytes())
//                );
//            }
//            List<CuratorTransactionResult> rets = zkClient.transaction().forOperations(ops);
//        } catch (Exception e) {
//            log.error("", e);
//        }
//    }

    public int getMaxPartid() {
        String path = prefix + "/maxpartid";
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
        String path = prefix + "/maxpartid";
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

//    public CuratorFramework getZkClient() { return zkClient; }
    public List<String> listQueue() {
        LinkedList<String> r = new LinkedList<>();
        String basePath = prefix + "/partitions";
        try {
            if (zkClient.checkExists().forPath(basePath) == null) {
                return r;
            }

            for(String q: zkClient.getChildren().forPath(basePath)){
                if(q.indexOf('~') > 0)
                    r.add(q);
            }
            return r;
        } catch (Exception e) {
            log.error("listQueue failed", e);
            return r;
        }
    }


    /**
     * 列出一个分区的全部数据
     * @param qid
     * @return
     */
    public List<PartConfig> getParts(String qid) {
        List<PartConfig> r = new LinkedList<>();
        String basePath = prefix + "/partitions/" + qid;
        try {
            if(zkClient.checkExists().forPath(basePath) == null){
                return r;
            }

            PartConfig c1 = new PartConfig();
            c1.setQid(qid);
            for(String partid: zkClient.getChildren().forPath(basePath)) {
                //- (part_id):  (part_num)~(part_status)~(ble_id)
                String path = basePath + "/" + partid;
                String data = new String(zkClient.getData().forPath(path));
                PartConfig c = c1.clone();
                c.setPartId(Integer.parseInt(partid));
                c.fromZKString(data);
                r.add(c);
            }
            return r;
        } catch (Exception e) {
            log.error("", e);
            return r;
        }
    }

    public void close() {
        if(zkClient != null)
            zkClient.close();
    }

    public boolean becomeSupervisor(String addr) {
        String basePath = prefix + "/" + "supervisor";
        try {
            zkClient.create().withMode(CreateMode.EPHEMERAL)
                    .forPath(basePath, addr.getBytes());
            return true;
        }catch(KeeperException.NodeExistsException e){
            return false;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public void watchBLEChange(final CallBack callback) {
        String path = prefix + "/" + "ble";
        try{
            zkClient.getChildren().usingWatcher(new CuratorWatcher() {
                @Override
                public void process(WatchedEvent watchedEvent) throws Exception {
                    watchBLEChange(callback);
                    callback.call();
                }
            }).forPath(path);
        }catch (Exception ex) {
            log.error("", ex);
        }
    }

    /**
     * 更新或创建分区数据
     * @param pc
     */
    public void setPart(PartConfig pc) {
        String path = prefix + "/partitions/" + pc.getQid() + "/" + pc.getPartId();
        try{
            if(zkClient.checkExists().forPath(path) == null){
                zkClient.create().creatingParentsIfNeeded().forPath(path,
                        pc.toZKString().getBytes());
            }else{
                zkClient.setData().forPath(path,
                        pc.toZKString().getBytes());
            }
        }catch (Exception ex) {
            log.error("", ex);
        }
    }

    public void delPart(PartConfig pc) {
        String path = prefix + "/partitions/" + pc.getQid() + "/" + pc.getPartId();
        try{
            if(zkClient.checkExists().forPath(path) != null){
                zkClient.delete().forPath(path);
            }
        }catch (Exception ex) {
            log.error("", ex);
        }
    }


    /**
     * 初始化分区变化标识
     */
    public void initPartChange() {
        String path = prefix + "/" + partChg;
        try{
            if(zkClient.checkExists().forPath(path) == null){
                zkClient.create().forPath(path,
                        String.valueOf(System.currentTimeMillis()).getBytes());
            }else{
                zkClient.setData().forPath(path,
                        String.valueOf(System.currentTimeMillis()).getBytes());
            }

            path = prefix + "/" + "partitions";
            if(zkClient.checkExists().forPath(path) == null) {
                zkClient.create().forPath(path,
                        "0".getBytes());
            }
        }catch (Exception ex) {
            log.error("", ex);
        }
    }

    /**
     * 更新分区变化标记
     */
    public void partChanged() {
        String path = prefix + "/" + partChg;
        try{
            zkClient.setData().forPath(path,
                String.valueOf(System.currentTimeMillis()).getBytes());

        }catch (Exception ex) {
            log.error("", ex);
        }
    }

    /**
     * 设置分区变化回调, 以便有分区变化使更新本地数据
     * @param callback
     */
    public void watchPartChange(final CallBack callback) {
        String path = prefix + "/" + partChg;
        try{
            zkClient.getData().usingWatcher(new CuratorWatcher() {
                @Override
                public void process(WatchedEvent watchedEvent) throws Exception {
                    watchPartChange(callback);
                    callback.call();
                }
            }).forPath(path);
        }catch (Exception ex) {
            log.error("", ex);
        }
    }

    /**
     * 清除全部的分区数据
     */
    public void clearParts() {
        String basePath = prefix + "/partitions";
        try {
            if (zkClient.checkExists().forPath(basePath) == null) {
                return;
            }

            for(String q: zkClient.getChildren().forPath(basePath)){
                if(q.indexOf('~') <= 0)
                    continue;
                String path = basePath + "/" + q;
                for(String p: zkClient.getChildren().forPath(path)) {
                    String pathp = path + "/" + p;
                    zkClient.delete().forPath(pathp);
                }
                zkClient.delete().forPath(path);
            }
            partChanged();
        } catch (Exception e) {
            log.error("clearParts failed", e);
        }
    }

}
