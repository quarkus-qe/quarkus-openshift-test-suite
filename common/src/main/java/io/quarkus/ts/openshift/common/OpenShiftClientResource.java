package io.quarkus.ts.openshift.common;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

final class OpenShiftClientResource implements CloseableResource {
    final OpenShiftClient client;

    static OpenShiftClientResource createDefault() {
        OpenShiftConfig config = new OpenShiftConfigBuilder()
                .withTrustCerts(true)
                .build();
        return new OpenShiftClientResource(new DefaultOpenShiftClient(config));
    }

    private OpenShiftClientResource(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public void close() {
        client.close();
    }
}
