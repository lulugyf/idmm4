package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.ActorRef;

/**
 * Created by guanyf on 5/9/2017.
 * 用于向个actor告知需要使用到的actorRef的
 */
public class RefMsg {
    final public String name;
    final public ActorRef ref;

    public RefMsg(String name, ActorRef ref) {
        this.name = name;
        this.ref = ref;
    }
}