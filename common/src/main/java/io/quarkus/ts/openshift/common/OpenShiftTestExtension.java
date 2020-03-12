package io.quarkus.ts.openshift.common;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.config.Config;
import io.quarkus.ts.openshift.common.injection.InjectionPoint;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.fusesource.jansi.Ansi.ansi;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;

// TODO at this point, this class is close to becoming unreadable, and could use some refactoring
final class OpenShiftTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        TestInstancePostProcessor, ParameterResolver,
        LifecycleMethodExecutionExceptionHandler, TestExecutionExceptionHandler {

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

    private AwaitUtil getAwaitUtil(ExtensionContext context) {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);
        return getStore(context)
                .getOrComputeIfAbsent(AwaitUtil.class.getName(), ignored -> new AwaitUtil(oc, metadata), AwaitUtil.class);
    }

    private OpenShiftUtil getOpenShiftUtil(ExtensionContext context) {
        OpenShiftClient oc = getOpenShiftClient(context);
        AwaitUtil await = getAwaitUtil(context);
        return getStore(context)
                .getOrComputeIfAbsent(OpenShiftUtil.class.getName(), ignogred -> new OpenShiftUtil(oc, await), OpenShiftUtil.class);
    }

    private void initTestsStatus(ExtensionContext context) {
        getStore(context).put(TestsStatus.class.getName(), new TestsStatus());
    }

    private TestsStatus getTestsStatus(ExtensionContext context) {
        TestsStatus testsStatus = getStore(context).get(TestsStatus.class.getName(), TestsStatus.class);
        if (testsStatus == null) {
            throw new IllegalStateException("missing " + TestsStatus.class.getSimpleName() + ", this is test framework bug");
        }
        return testsStatus;
    }

    // ---

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.out.println("---------- OpenShiftTest set up ----------");

        initTestsStatus(context);

        createEphemeralNamespaceIfNecessary(context);

        AppMetadata metadata = getAppMetadata(context);

        Path openshiftResources = getResourcesYaml();
        if (!Files.exists(openshiftResources)) {
            throw new OpenShiftTestException("Missing " + openshiftResources + ", did you add the quarkus-kubernetes or quarkus-openshift extension?");
        }

        deployAdditionalResources(context);

        System.out.println("deploying application");
        new Command("oc", "apply", "-f", openshiftResources.toString()).runAndWait();

        awaitImageStreams(context);

        new Command("oc", "start-build", metadata.appName, "--from-dir=target", "--follow").runAndWait();

        setUpRestAssured(context);

        getAwaitUtil(context).awaitAppRoute();
    }

    private void createEphemeralNamespaceIfNecessary(ExtensionContext context) throws IOException, InterruptedException {
        if (EphemeralNamespace.isEnabled()) {
            EphemeralNamespace namespace = EphemeralNamespace.newWithRandomName();
            getStore(context).put(EphemeralNamespace.class.getName(), namespace);

            System.out.println(ansi().a("using ephemeral namespace ").fgYellow().a(namespace.name).reset());
            new Command("oc", "new-project", namespace.name).runAndWait();
        }
    }

    private void deployAdditionalResources(ExtensionContext context) throws IOException, InterruptedException {
        TestsStatus testsStatus = getTestsStatus(context);
        Optional<AnnotatedElement> element = context.getElement();
        if (element.isPresent()) {
            AnnotatedElement annotatedElement = element.get();
            AdditionalResources[] annotations = annotatedElement.getAnnotationsByType(AdditionalResources.class);
            for (AdditionalResources additionalResources : annotations) {
                AdditionalResourcesDeployed deployed = AdditionalResourcesDeployed.deploy(additionalResources, testsStatus);

                if (EphemeralNamespace.isDisabled()) {
                    // when using ephemeral namespaces, we don't delete additional resources because:
                    // - when an ephemeral namespace is dropped, everything is destroyed anyway
                    // - when retain on failure is enabled and failure occurs,
                    //   everything in the ephemeral namespace must be kept intact
                    getStore(context).put(new Object(), deployed);
                }
            }
        }
    }

    private void awaitImageStreams(ExtensionContext context) {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);
        AwaitUtil awaitUtil = getAwaitUtil(context);

        oc.imageStreams()
                .list()
                .getItems()
                .stream()
                .map(it -> it.getMetadata().getName())
                .filter(it -> !it.equals(metadata.appName))
                .forEach(awaitUtil::awaitImageStream);
    }

    private void setUpRestAssured(ExtensionContext context) throws Exception {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);
        Route route = oc.routes().withName(metadata.appName).get();
        if (route == null) {
            throw new OpenShiftTestException("Missing route " + metadata.appName + ", did you set quarkus.openshift.expose=true?");
        }
        if (route.getSpec().getTls() != null) {
            RestAssured.useRelaxedHTTPSValidation();
            RestAssured.baseURI = "https://" + route.getSpec().getHost();
        } else {
            RestAssured.baseURI = "http://" + route.getSpec().getHost();
        }
        RestAssured.basePath = metadata.httpRoot;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        System.out.println("---------- OpenShiftTest tear down ----------");

        boolean shouldUndeployApplication = true;
        if (EphemeralNamespace.isEnabled()) {
            // when using ephemeral namespaces, we don't undeploy the application because:
            // - when an ephemeral namespace is dropped, everything is destroyed anyway
            // - when retain on failure is enabled and failure occurs,
            //   everything in the ephemeral namespace must be kept intact
            shouldUndeployApplication = false;
        }
        if (RetainOnFailure.isEnabled() && getTestsStatus(context).failed) {
            shouldUndeployApplication = false;

            if (EphemeralNamespace.isDisabled()) {
                // when using ephemeral namespaces, a different message will be printed in dropEphemeralNamespaceIfNecessary
                System.out.println(ansi().a("test ").fgYellow().a(context.getDisplayName()).reset()
                        .a(" failed, not deleting any resources"));
            }
        }

        if (shouldUndeployApplication) {
            System.out.println("undeploying application");
            new Command("oc", "delete", "-f", getResourcesYaml().toString(), "--ignore-not-found").runAndWait();
        }

        dropEphemeralNamespaceIfNecessary(context);
    }

    private void dropEphemeralNamespaceIfNecessary(ExtensionContext context) throws IOException, InterruptedException {
        EphemeralNamespace ephemeralNamespace = getStore(context).get(EphemeralNamespace.class.getName(), EphemeralNamespace.class);
        TestsStatus status = getTestsStatus(context);
        if (ephemeralNamespace != null) {
            if (RetainOnFailure.isEnabled() && status.failed) {
                System.out.println(ansi().a("test ").fgYellow().a(context.getDisplayName()).reset()
                        .a(" failed, keeping ephemeral namespace ").fgYellow().a(ephemeralNamespace.name).reset()
                        .a(" intact"));
            } else {
                System.out.println(ansi().a("dropping ephemeral namespace ").fgYellow().a(ephemeralNamespace.name).reset());
                new Command("oc", "delete", "project", ephemeralNamespace.name).runAndWait();
            }
        }
    }

    // ---

    @Override
    public void beforeEach(ExtensionContext context) {
        System.out.println(ansi().a("---------- running test ")
                .fgYellow().a(context.getParent().map(ctx -> ctx.getDisplayName() + ".").orElse(""))
                .a(context.getDisplayName()).reset().a("----------"));
    }

    // ---

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
        } else if (AwaitUtil.class.equals(injectionPoint.type())) {
            return getAwaitUtil(context);
        } else if (OpenShiftUtil.class.equals(injectionPoint.type())) {
            return getOpenShiftUtil(context);
        } else if (Config.class.equals(injectionPoint.type())) {
            return Config.get();
        } else {
            throw new OpenShiftTestException("Unsupported type " + injectionPoint.type().getSimpleName()
                    + " for @TestResource " + injectionPoint.description());
        }
    }

    // ---

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        failureOccured(context);
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        failureOccured(context);
        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        failureOccured(context);
        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        failureOccured(context);
        throw throwable;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        failureOccured(context);
        throw throwable;
    }

    private void failureOccured(ExtensionContext context) {
        getTestsStatus(context).failed = true;
    }
}
