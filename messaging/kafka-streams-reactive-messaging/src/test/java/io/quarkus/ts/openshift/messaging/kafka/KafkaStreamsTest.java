package io.quarkus.ts.openshift.messaging.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;

@QuarkusTest
@QuarkusTestResource(StrimziKafkaResource.class)
public class KafkaStreamsTest extends AbstractKafkaTest{

    private static final int ssePort = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);

    @Override
    public String getEndpoint(){
        return String.format("http://localhost:%d", ssePort);
    }
}
