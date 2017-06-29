package com.sitech.crmpd.idmm.supervisor;

import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.supervisor.stru.BLEState;
import com.sitech.crmpd.idmm.util.ZK;
import io.netty.bootstrap.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by guanyf on 6/12/2017.
 */
public class Tools {
    private static final Logger log = LoggerFactory.getLogger(Tools.class);


    /**
     * 处理zookeeper上ble列表的变更
     * 新增
     * @param bles
     * @param zk
     * @param bootstrap
     * @return
     */
    public static void bleListChg(Map<String, BLEState> bles, ZK zk, Bootstrap bootstrap, ActorRef ref) {
//        bles.clear();

        Map<String, String> m = zk.getBLEList();
        if(m == null) {
            log.error("can not get ble list from zk");
            return;
        }

        // 检查新增的ble
        for(String bleid: m.keySet()) {
            if(bles.containsKey(bleid))
                continue;

            // new BLE
            BLEState b = new BLEState();
            b.id = bleid;
            b.cmdaddr = m.get(bleid);
            try{
                String v = b.cmdaddr;
                int p = v.indexOf(':');
                log.debug("begin connect to ble {}", bleid);
                b.ch = bootstrap.connect(v.substring(0, p), Integer.parseInt(v.substring(p+1))).sync().channel();
                log.debug("ble {} connected", bleid);
                b.isOk = true;
                bles.put(bleid, b);
                // done new ble online, send query for parts
                ref.tell(new String[]{"ble", bleid}, ActorRef.noSender());
            }catch(Exception ex){
                log.error("create ble connection failed", ex);
                // TODO need to remove the ble item
                zk.removeBLE(bleid);
            }
        }

        // 检查离线的ble
        for(String bleid: bles.keySet()){
            if(m.containsKey(bleid))
                continue;

            BLEState b = bles.remove(bleid);
            log.warn("remove BLE {}", bleid);
            try {
                b.ch.close().sync();
            } catch (InterruptedException e) {
                log.error("close channel for bleid {} failed", bleid, e);
            }
        }
    }

}
