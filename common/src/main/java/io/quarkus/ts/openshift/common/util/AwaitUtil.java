package io.quarkus.ts.openshift.common.util;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.fusesource.jansi.Ansi.ansi;

public final class AwaitUtil {
    private final OpenShiftClient oc;
    private final AppMetadata metadata;

    public AwaitUtil(OpenShiftClient oc, AppMetadata metadata) {
        this.oc = oc;
        this.metadata = metadata;
    }

    public void awaitImageStream(String imageStream) {
        System.out.println(ansi().a("waiting for image stream ").fgYellow().a(imageStream).reset().a(" to populate"));
        await().atMost(5, TimeUnit.MINUTES).until(imageStreamHasTags(oc, imageStream));
    }

    private static Callable<Boolean> imageStreamHasTags(OpenShiftClient oc, String imageStream) {
        return () -> !oc.imageStreams().withName(imageStream).get().getSpec().getTags().isEmpty();
    }

    public void awaitAppRoute() {
        System.out.println(ansi().a("waiting for route ").fgYellow().a(metadata.appName).reset()
                .a(" to start responding at ").fgYellow().a(metadata.knownEndpoint).reset());
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            given()
                    // known endpoint is already httpRoot-adjusted
                    .basePath("/")
            .when()
                    .get(metadata.knownEndpoint)
            .then()
                    .statusCode(200);
        });
    }
}
