package com.sitech.crmpd.idmm.ble.actor;

import akka.actor.ActorRef;

/**
 * Created by guanyf on 5/9/2017.
 * 用于向个actor告知需要使用到的actorRef的
 */
public class RefMsg {
    final public String name;
    final public ActorRef ref;
    final public Object obj;

    public RefMsg(String name, ActorRef ref, Object obj) {
        this.name = name;
        this.ref = ref;
        this.obj = obj;
    }
    public RefMsg(String name, ActorRef ref) {
        this.name = name;
        this.ref = ref;
        this.obj = null;
    }
}
