package com.sitech.crmpd.idmm.ble.main.spring;

import akka.actor.*;
import org.springframework.context.ApplicationContext;

/**
 * Created by guanyf on 7/6/2017.
 *  integrate spring with actor
 *  based on http://www.baeldung.com/akka-with-spring
 */
public class SpringExtension  extends AbstractExtensionId<SpringExtension.SpringExt> {

    public static final SpringExtension SPRING_EXTENSION_PROVIDER
            = new SpringExtension();

    @Override
    public SpringExt createExtension(ExtendedActorSystem system) {
        return new SpringExt();
    }

    public static class SpringExt implements Extension {
        private volatile ApplicationContext applicationContext;

        public void initialize(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        public Props props(Class<? extends Actor> c, Object... args) {
            return Props.create(
                    SpringActorProducer.class, applicationContext, c, args);
        }
    }
}