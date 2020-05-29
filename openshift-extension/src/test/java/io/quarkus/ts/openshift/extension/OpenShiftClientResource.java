package io.quarkus.ts.openshift.extension;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

final class OpenShiftClientResource implements CloseableResource {
    final OpenShiftClient client;

    static OpenShiftClientResource createDefault() {
        return new OpenShiftClientResource(new DefaultOpenShiftClient());
    }

    private OpenShiftClientResource(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public void close() {
        client.close();
    }
}
