package io.quarkus.ts.openshift.http;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.ts.openshift.common.resources.KeycloakQuarkusTestResource;
import org.eclipse.microprofile.config.ConfigProvider;

@QuarkusTest
@QuarkusTestResource(KeycloakQuarkusTestResource.WithOidcConfig.class)
public class HttpTest extends AbstractHttpTest {

    private static final int appPort = ConfigProvider.getConfig().getValue("quarkus.http.test-ssl-port", Integer.class);

    @Override
    protected String getAppEndpoint() {
        return String.format("https://localhost:%d/api/", appPort);
    }
}
