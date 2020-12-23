package io.quarkus.ts.openshift.http;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;

@QuarkusTest
public class HttpTest extends AbstractHttpTest {

    private static final int appPort = ConfigProvider.getConfig().getValue("quarkus.http.test-ssl-port", Integer.class);

    @Override
    protected String getAppEndpoint() {
        return String.format("https://localhost:%d/api", appPort);
    }
}
