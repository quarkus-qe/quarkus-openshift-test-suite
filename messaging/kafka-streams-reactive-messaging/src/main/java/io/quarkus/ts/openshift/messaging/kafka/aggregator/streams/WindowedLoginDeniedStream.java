package io.quarkus.ts.openshift.messaging.kafka.aggregator.streams;

import io.quarkus.kafka.client.serialization.JsonbSerde;
import io.quarkus.ts.openshift.messaging.kafka.aggregator.model.LoginAggregation;
import io.quarkus.ts.openshift.messaging.kafka.aggregator.model.LoginAttempt;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import java.time.Duration;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@ApplicationScoped
public class WindowedLoginDeniedStream {

    static final String LOGIN_AGGREGATION_STORE = "login-aggregation-store";
    static final String LOGIN_ATTEMPTS_TOPIC = "login-http-response-values";
    static final String LOGIN_DENIED_AGGREGATED_TOPIC = "login-denied";

    @ConfigProperty(name = "login.denied.windows.sec")
    int windowsLoginSec;

    @ConfigProperty(name = "login.denied.threshold")
    int threshold;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        JsonbSerde<LoginAttempt> loginAttemptSerde = new JsonbSerde<>(LoginAttempt.class);
        JsonbSerde<LoginAggregation> loginAggregationSerde = new JsonbSerde<>(LoginAggregation.class);

        builder.stream(LOGIN_ATTEMPTS_TOPIC, Consumed.with(Serdes.String(), loginAttemptSerde))
                .groupByKey()
                .windowedBy(TimeWindows.of(Duration.ofSeconds(windowsLoginSec)))
                .aggregate(LoginAggregation::new,
                        (id, value, aggregation) -> aggregation.updateFrom(value),
                        Materialized.<String, LoginAggregation, WindowStore<Bytes, byte[]>> as(LOGIN_AGGREGATION_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(loginAggregationSerde))
                .toStream()
                .filter((k, v) -> (v.getCode() == UNAUTHORIZED.getStatusCode() || v.getCode() == FORBIDDEN.getStatusCode()))
                .filter((k,v) -> v.getCount() > threshold)
                .to(LOGIN_DENIED_AGGREGATED_TOPIC);

        return builder.build();
    }

    @Incoming(LOGIN_DENIED_AGGREGATED_TOPIC)
    @Outgoing("login-alerts")
    @Broadcast
    public String fanOut(String jsonLoginAggregation) {
        return jsonLoginAggregation;
    }
}
