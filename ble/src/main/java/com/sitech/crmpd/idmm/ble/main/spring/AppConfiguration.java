package com.sitech.crmpd.idmm.ble.main.spring;

import akka.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static com.sitech.crmpd.idmm.ble.main.spring.SpringExtension.SPRING_EXTENSION_PROVIDER;

/**
 * Created by guanyf on 7/6/2017.
 */
@Configuration
@ComponentScan
public class AppConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("root");
        SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }
}
