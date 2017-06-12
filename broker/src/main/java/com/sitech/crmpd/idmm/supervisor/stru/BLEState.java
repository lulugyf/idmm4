package com.sitech.crmpd.idmm.supervisor.stru;

import com.sitech.crmpd.idmm.cfg.PartConfig;
import io.netty.channel.Channel;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by guanyf on 6/12/2017.
 * 一个 BLE 实例的结构信息
 */
public class BLEState implements Comparable<BLEState>{
    public Channel ch;
    public String id;
    public String cmdaddr;
    public boolean isOk;

    public List<PartConfig> parts = new LinkedList<>();
    public long lastHeartbeat; //最后心跳时间
    public long query_stat = 0L; //查询时间， 为0则没有执行过查询

    @Override
    public int compareTo(BLEState o) {
        return o.parts.size() - parts.size();
    }
}
