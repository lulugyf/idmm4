package com.sitech.crmpd.idmm.ble.main.spring;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

/**
 * Created by guanyf on 7/6/2017.
 */
public class SpringActorProducer implements IndirectActorProducer {

    private ApplicationContext applicationContext;

    private Class<? extends Actor> beanClass;

    private Object[] args;

    public SpringActorProducer(ApplicationContext applicationContext,
                                Class<? extends Actor> v, Object...args) {
        this.applicationContext = applicationContext;
        this.beanClass = v;
        this.args = args;
    }

    @Override
    public Actor produce() {
        return (Actor) applicationContext.getBean(beanClass, args);
    }

    @Override
    public Class<? extends Actor> actorClass() {
//        return (Class<? extends Actor>) applicationContext
//                .getType(beanActorName);
        return beanClass;
    }
}