package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.ActorRef;
import io.netty.channel.Channel;

/**
 * Created by guanyf on 5/9/2017.
 * 用于向个actor告知需要使用到的actorRef的
 */
public class RefMsg {
    final public String name;
    final public ActorRef ref;
    final public String str;
    final public Channel ch;
    final public Object obj;

    public RefMsg(String name, ActorRef ref) {
        this(name, ref, null, null, null);
    }
    public RefMsg(String name, ActorRef ref, String str, Channel ch) {
        this(name, ref, str, ch, null);
    }
    public RefMsg(String name, ActorRef ref, String str, Channel ch, Object obj) {
        this.name = name;
        this.ref = ref;
        this.str = str;
        this.ch = ch;
        this.obj = obj;
    }
}
