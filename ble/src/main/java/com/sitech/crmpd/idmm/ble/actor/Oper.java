package com.sitech.crmpd.idmm.ble.actor;

import com.sitech.crmpd.idmm.ble.mem.MsgIndex;
import io.netty.channel.Channel;

/**
 * Created by guanyf on 3/17/2017.
 */
final public class Oper {
    public OType type;

    public Channel channel; // 应答通道
//    public volatile Object oneQ;    // 回送给OneThread 请求的队列

    public String msgid;
    public MsgIndex mi;
    public int seq;
    public int process_time;
    public int ret = -1; // 0 success
    public long create_time;
    public boolean next = false;

    public Oper(OType t) {
        this.type = t;
        create_time = System.currentTimeMillis();
    }
    public void reset() {
        type = null;
        channel = null;
        msgid = null;
        mi = null;
        ret = -1;
    }

    public static enum OType {
        addOP,
        addOP1,

        getOP,

        ackOP,
        ackOP1,

        failOP,
        skipOP
    }
}
