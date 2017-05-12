package com.sitech.crmpd.idmm.ble.actor;

import com.sitech.crmpd.idmm.ble.mem.MsgIndex;
import io.netty.channel.Channel;

/**
 * Created by guanyf on 3/17/2017.
 */
final public class Oper {
    public volatile OType type;

    public volatile Channel channel; // 应答通道
    public volatile Object oneQ;    // 回送给OneThread 请求的队列

    public volatile String msgid;
    public volatile MsgIndex mi;
    public volatile int seq;
    public volatile long process_time;
    public volatile int ret = -1; // 0 success
    public long create_time;

    public Oper(OType t) {
        this.type = t;
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
