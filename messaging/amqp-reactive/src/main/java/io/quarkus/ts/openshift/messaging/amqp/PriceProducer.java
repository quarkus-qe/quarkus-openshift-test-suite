package io.quarkus.ts.openshift.messaging.amqp;

import io.reactivex.Flowable;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PriceProducer {

    private static final Logger LOG = Logger.getLogger(PriceProducer.class.getName());

    @Outgoing("generated-price")
    public Flowable<Integer> generate() {
        LOG.info("generate fired...");
        return Flowable.interval(1, TimeUnit.SECONDS)
                .map(tick -> ((tick.intValue() * 10) % 100) + 10);
    }
}
