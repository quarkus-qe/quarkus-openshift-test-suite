package io.quarkus.ts.openshift.messaging.qpid;

import static org.awaitility.Awaitility.await;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ArtemisTestResource implements QuarkusTestResourceLifecycleManager {
    private EmbeddedActiveMQ embedded;

    @Override
    public Map<String, String> start() {
        try {
            FileUtils.deleteDirectory(Paths.get("./target/artemis").toFile());
            embedded = new EmbeddedActiveMQ();
            embedded.start();

            await().atMost(5, TimeUnit.MINUTES).until(() -> embedded.getActiveMQServer() != null
                    && embedded.getActiveMQServer().isActive()
                    && embedded.getActiveMQServer().getConnectorsService().isStarted());
        } catch (Exception e) {
            throw new RuntimeException("Could not start embedded ActiveMQ Artemis server", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        try {
            embedded.stop();
        } catch (Exception e) {
            throw new RuntimeException("Could not stop embedded ActiveMQ Artemis server", e);
        }
    }
}
