package io.quarkus.ts.openshift.common;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.injection.InjectionPoint;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.fusesource.jansi.Ansi.ansi;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;

final class OpenShiftTestExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor, ParameterResolver {
    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass()));
    }

    private OpenShiftClient getOpenShiftClient(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(OpenShiftClientResource.class.getName(), ignored -> OpenShiftClientResource.createDefault(), OpenShiftClientResource.class)
                .client;
    }

    private Path getResourcesYaml() {
        return Paths.get("target", "kubernetes", "openshift.yml");
    }

    private AppMetadata getAppMetadata(ExtensionContext context) {
        Path file = Paths.get("target", "app-metadata.properties");
        return getStore(context)
                .getOrComputeIfAbsent(AppMetadata.class.getName(), ignored -> AppMetadata.load(file), AppMetadata.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Path openshiftResources = getResourcesYaml();
        if (!Files.exists(openshiftResources)) {
            throw new OpenShiftTestException("Missing " + openshiftResources + ", did you add the quarkus-kubernetes extension?");
        }

        AppMetadata metadata = getAppMetadata(context);

        new Command("oc apply", "oc", "apply", "-f", openshiftResources.toString()).runAndWait();

        awaitImageStreams(context);

        new Command("oc start-build", "oc", "start-build", metadata.appName, "--from-dir=target", "--follow").runAndWait();

        setUpRestAssured(context);

        awaitRoute(context);
    }

    private void awaitImageStreams(ExtensionContext context) {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);

        oc.imageStreams()
                .list()
                .getItems()
                .stream()
                .map(it -> it.getMetadata().getName())
                .filter(it -> !it.equals(metadata.appName))
                .forEach(imageStream -> {
                    System.out.println(ansi().a("waiting for image stream ").fgYellow().a(imageStream).reset()
                            .a(" to populate"));
                    await().atMost(5, TimeUnit.MINUTES).until(imageStreamHasTags(oc, imageStream));
                });
    }

    private Callable<Boolean> imageStreamHasTags(OpenShiftClient oc, String imageStream) {
        return () -> !oc.imageStreams().withName(imageStream).get().getSpec().getTags().isEmpty();
    }

    private void setUpRestAssured(ExtensionContext context) throws Exception {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);
        Route route = oc.routes().withName(metadata.appName).get();
        if (route == null) {
            throw new OpenShiftTestException("Missing route " + metadata.appName + ", did you set openshift.expose=true?");
        }
        if (route.getSpec().getTls() != null) {
            RestAssured.useRelaxedHTTPSValidation();
            RestAssured.baseURI = "https://" + route.getSpec().getHost();
        } else {
            RestAssured.baseURI = "http://" + route.getSpec().getHost();
        }
        RestAssured.basePath = metadata.httpRoot;
    }

    private void awaitRoute(ExtensionContext context) {
        AppMetadata metadata = getAppMetadata(context);
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

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        new Command("oc delete", "oc", "delete", "-f", getResourcesYaml().toString()).runAndWait();
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for (Field field : findAnnotatedFields(testInstance.getClass(), TestResource.class, ignored -> true)) {
            InjectionPoint injectionPoint = InjectionPoint.forField(field);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(testInstance, valueFor(injectionPoint, context));
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) throws ParameterResolutionException {
        return parameter.isAnnotated(TestResource.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) throws ParameterResolutionException {
        InjectionPoint injectionPoint = InjectionPoint.forParameter(parameter.getParameter());
        try {
            return valueFor(injectionPoint, context);
        } catch (OpenShiftTestException e) {
            throw new ParameterResolutionException(e.getMessage(), e);
        }
    }

    private Object valueFor(InjectionPoint injectionPoint, ExtensionContext context) throws OpenShiftTestException {
        if (OpenShiftClient.class.equals(injectionPoint.type())) {
            return getOpenShiftClient(context);
        } else if (AppMetadata.class.equals(injectionPoint.type())) {
            return getAppMetadata(context);
        } else {
            throw new OpenShiftTestException("Unsupported type for @TestResource " + injectionPoint.description());
        }
    }
}
