package io.quarkus.ts.openshift.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Handlers;
import io.fabric8.kubernetes.client.ResourceHandler;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.OpenShiftTestException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
        // if the route responds with 200, that should imply readiness
        // unfortunately, with OpenShift 4.6, that doesn't seem to be the case :-/
        awaitReadiness(Arrays.asList(
                oc.deploymentConfigs().withName(metadata.appName).get(),
                oc.apps().deployments().withName(metadata.appName).get()
        ));

        System.out.println(ansi().a("waiting for route ").fgYellow().a(metadata.appName).reset()
                .a(" to start responding at ").fgYellow().a(metadata.knownEndpoint).reset());
        await().ignoreExceptions().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            given()
                    // known endpoint is already httpRoot-adjusted
                    .basePath("/")
            .when()
                    .get(metadata.knownEndpoint)
            .then()
                    .statusCode(200);
        });
    }

    public void awaitReadiness(List<HasMetadata> resources) {
        resources.stream()
                .filter(Objects::nonNull)
                .filter(ReadinessUtil.INSTANCE::isReadinessApplicable)
                .forEach(it -> {
                    System.out.println(ansi().a("waiting for ").a(readableKind(it.getKind())).a(" ")
                            .fgYellow().a(it.getMetadata().getName()).reset().a(" to become ready"));
                    await().pollInterval(1, TimeUnit.SECONDS)
                           .atMost(5, TimeUnit.MINUTES)
                           .until(() -> {
                        HasMetadata current = oc.resource(it).fromServer().get();
                        if (current == null) {
                            ResourceHandler<HasMetadata, ?> handler = Handlers.get(it.getKind(), it.getApiVersion());
                            if (handler != null && !handler.getApiVersion().equals(it.getApiVersion())) {
                                throw new OpenShiftTestException("Couldn't load " + readableKind(it.getKind()) + " '"
                                        + it.getMetadata().getName() + "' from API server, most likely because"
                                        + " the 'apiVersion' doesn't match: has '" + it.getApiVersion() + "', but"
                                        + " should have '" + handler.getApiVersion() + "'");
                            }
                            throw new OpenShiftTestException("Couldn't load " + readableKind(it.getKind()) + " '"
                                    + it.getMetadata().getName() + "' from API server");
                        }
                        return ReadinessUtil.INSTANCE.isReady(current);
                    });
                });
    }

    // DeploymentConfig -> deployment config
    private static String readableKind(String kind) {
        StringBuilder result = new StringBuilder(kind.length());
        boolean shouldAppendSpaceAfterUpperCase = false;
        for (int i = 0; i < kind.length(); i++) {
            char c = kind.charAt(i);
            if (Character.isUpperCase(c)) {
                if (shouldAppendSpaceAfterUpperCase) {
                    result.append(' ');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
            shouldAppendSpaceAfterUpperCase = true; // only false for the first character
        }
        return result.toString();
    }
}
