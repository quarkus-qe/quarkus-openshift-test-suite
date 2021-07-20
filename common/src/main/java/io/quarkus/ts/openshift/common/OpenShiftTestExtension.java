package io.quarkus.ts.openshift.common;

import static org.fusesource.jansi.Ansi.ansi;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.fusesource.jansi.Ansi;
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

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.app.metadata.AppMetadata;
import io.quarkus.ts.openshift.common.actions.OnOpenShiftFailureAction;
import io.quarkus.ts.openshift.common.config.Config;
import io.quarkus.ts.openshift.common.deploy.DeploymentStrategy;
import io.quarkus.ts.openshift.common.deploy.OpenShiftDeploymentStrategyLoader;
import io.quarkus.ts.openshift.common.injection.InjectionPoint;
import io.quarkus.ts.openshift.common.injection.TestResource;
import io.quarkus.ts.openshift.common.injection.WithName;
import io.quarkus.ts.openshift.common.util.AwaitUtil;
import io.quarkus.ts.openshift.common.util.OpenShiftUtil;
import io.restassured.RestAssured;

// TODO at this point, this class is close to becoming unreadable, and could use some refactoring.
// Raised https://github.com/quarkus-qe/quarkus-openshift-test-suite/issues/108 for the refactoring.
final class OpenShiftTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        TestInstancePostProcessor, ParameterResolver,
        LifecycleMethodExecutionExceptionHandler, TestExecutionExceptionHandler {

    static {
        try (InputStream in = OpenShiftTestExtension.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Could not load logging.properties", e);
        }
    }

    private final ServiceLoader<OnOpenShiftFailureAction> onFailureActions = ServiceLoader.load(OnOpenShiftFailureAction.class);

    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass()));
    }

    private Optional<CustomAppMetadata> getCustomAppMetadataAnnotation(ExtensionContext context) {
        return context.getElement().map(it -> it.getAnnotation(CustomAppMetadata.class));
    }

    private OpenShiftClient getOpenShiftClient(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(OpenShiftClientResource.class.getName(),
                        ignored -> OpenShiftClientResource.createDefault(), OpenShiftClientResource.class).client;
    }

    private AppMetadata getAppMetadata(ExtensionContext context) {
        Optional<CustomAppMetadata> customAppMetadata = getCustomAppMetadataAnnotation(context);
        if (customAppMetadata.isPresent()) {
            return getStore(context)
                    .getOrComputeIfAbsent(AppMetadata.class.getName(), ignored -> new AppMetadata(
                            customAppMetadata.get().appName(),
                            customAppMetadata.get().httpRoot(),
                            customAppMetadata.get().knownEndpoint(),
                            customAppMetadata.get().deploymentTarget()), AppMetadata.class);
        }

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
                .getOrComputeIfAbsent(OpenShiftUtil.class.getName(), ignored -> new OpenShiftUtil(oc, await),
                        OpenShiftUtil.class);
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
        initTestsStatus(context);

        try {
            doBeforeAll(context);
        } catch (Exception e) {
            getTestsStatus(context).failed = true;
            throw e;
        }
    }

    private void doBeforeAll(ExtensionContext context) throws Exception {
        System.out.println("---------- OpenShiftTest set up ----------");
        createEphemeralNamespaceIfNecessary(context);
        CompletableFuture.allOf(deployAdditionalResources(context)).join();
        runPublicStaticVoidMethods(CustomizeApplicationDeployment.class, context);

        Map<String, String> envVars = resolveEnvVars(context);
        getDeploymentStrategy(context).deploy(envVars);
        setUpRestAssured(context);
        getAwaitUtil(context).awaitAppRoute();
    }

    private Map<String, String> resolveEnvVars(ExtensionContext context) throws OpenShiftTestException {
        Map<String, String> envVars = new HashMap<>();
        Optional<AnnotatedElement> element = context.getElement();
        if (element.isPresent()) {
            OpenShiftUtil openshift = getOpenShiftUtil(context);
            AnnotatedElement annotatedElement = element.get();
            InjectRouteUrlIntoApp[] annotations = annotatedElement.getAnnotationsByType(InjectRouteUrlIntoApp.class);
            for (InjectRouteUrlIntoApp annotation : annotations) {
                String value = openshift.getUrlFromRoute(annotation.route());
                envVars.put(annotation.envVar(), value);
            }
        }

        return envVars;
    }

    private void createEphemeralNamespaceIfNecessary(ExtensionContext context) throws IOException, InterruptedException {
        if (EphemeralNamespace.isEnabled()) {
            EphemeralNamespace namespace = EphemeralNamespace.newWithRandomName();
            getStore(context).put(EphemeralNamespace.class.getName(), namespace);

            System.out.println(ansi().a("using ephemeral namespace ").fgYellow().a(namespace.name).reset());
            new Command("oc", "new-project", namespace.name).runAndWait();
        }
    }

    private CompletableFuture<Void> deployAdditionalResources(ExtensionContext context) {
        final String actionName = "deployAdditionalResources";
        TestsStatus testsStatus = getTestsStatus(context);
        OpenShiftClient oc = getOpenShiftClient(context);
        AwaitUtil awaitUtil = getAwaitUtil(context);
        Optional<AnnotatedElement> element = context.getElement();
        List<CompletableFuture<Pair<String, Boolean>>> additionalResourceStatus = new ArrayList<>();
        if (element.isPresent()) {
            AnnotatedElement annotatedElement = element.get();
            AdditionalResources[] annotations = annotatedElement.getAnnotationsByType(AdditionalResources.class);
            boolean isParallelDeployment = isParallelDeploymentEnabled(annotatedElement);
            for (AdditionalResources additionalResources : annotations) {
                Supplier<Pair<String, Boolean>> deployStatus = applyDeployment(context, testsStatus, oc, awaitUtil,
                        additionalResources);
                if (isParallelDeployment) {
                    additionalResourceStatus.add(CompletableFuture.supplyAsync(() -> deployStatus.get()));
                } else {
                    additionalResourceStatus.add(CompletableFuture.completedFuture(deployStatus.get()));
                }
            }
        }
        CompletableFuture[] additionalResources = additionalResourceStatus.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(additionalResources)
                .thenAccept(next -> printResults(actionName, additionalResourceStatus));
    }

    private boolean isParallelDeploymentEnabled(AnnotatedElement annotatedElement) {
        return annotatedElement.getAnnotationsByType(ParallelAdditionalResourcesEnabled.class).length > 0;
    }

    private Supplier<Pair<String, Boolean>> applyDeployment(ExtensionContext context, TestsStatus testsStatus,
            OpenShiftClient oc, AwaitUtil awaitUtil, AdditionalResources additionalResources) {
        return () -> {
            String resourceUrl = additionalResources.value();
            Pair<String, Boolean> status = MutablePair.of(resourceUrl, true);
            try {
                AdditionalResourcesDeployed deployed = AdditionalResourcesDeployed.deploy(additionalResources, testsStatus, oc,
                        awaitUtil);
                if (EphemeralNamespace.isDisabled()) {
                    // when using ephemeral namespaces, we don't delete additional resources because:
                    // - when an ephemeral namespace is dropped, everything is destroyed anyway
                    // - when retain on failure is enabled and failure occurs,
                    //   everything in the ephemeral namespace must be kept intact
                    getStore(context).put(new Object(), deployed);
                }
                return status;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                status.setValue(false);
                return status;
            }
        };
    }

    private void printResults(String title, List<CompletableFuture<Pair<String, Boolean>>> results) {
        System.out.println(ansi().a(String.format("--- Summary: %s ---", title)));
        for (CompletableFuture<Pair<String, Boolean>> result : results) {
            try {
                Ansi content = ansi().a(result.get().getKey() + " ");
                content = result.get().getValue() ? content.fgGreen().a("\u2713") : content.fgRed().a("\u2717");
                System.out.println(content.reset());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            }
        }
        System.out.println(ansi().a("--- end ---"));
    }

    private void setUpRestAssured(ExtensionContext context) throws Exception {
        OpenShiftClient oc = getOpenShiftClient(context);
        AppMetadata metadata = getAppMetadata(context);

        if (metadata.deploymentTarget.isEmpty() || !metadata.deploymentTarget.contains("knative")) {
            System.out.println(ansi().a("using ").fgYellow().a("OpenShiftClient").reset().a(" to get the route"));

            Route route = oc.routes().withName(metadata.appName).get();
            if (route == null) {
                throw new OpenShiftTestException(
                        "Missing route " + metadata.appName + ", did you set quarkus.openshift.route.expose=true?");
            }
            if (route.getSpec().getTls() != null) {
                RestAssured.useRelaxedHTTPSValidation();
                RestAssured.baseURI = "https://" + route.getSpec().getHost();
            } else {
                RestAssured.baseURI = "http://" + route.getSpec().getHost();
            }
        } else {
            System.out.println(ansi().a("using ").fgYellow().a("KnativeClient").reset().a(" to get the route"));

            KnativeClient kn = oc.adapt(KnativeClient.class);
            io.fabric8.knative.serving.v1.Route knRoute = kn.routes().withName(metadata.appName).get();
            if (knRoute == null) {
                throw new OpenShiftTestException("Missing route " + metadata.appName);
            }
            RestAssured.baseURI = knRoute.getStatus().getUrl();
        }
        RestAssured.basePath = metadata.httpRoot;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        boolean testsFailed = getTestsStatus(context).failed;

        if (testsFailed) {
            System.out.println("---------- OpenShiftTest failure ----------");
            System.out.println(ansi().a("test ").fgYellow().a(context.getDisplayName()).reset()
                    .a(" failed, showing current namespace status"));

            onFailureActions.forEach(action -> this.runOnFailureAction(context, action));
        }

        System.out.println("---------- OpenShiftTest tear down ----------");

        if (!(testsFailed && RetainOnFailure.isEnabled())) {
            getDeploymentStrategy(context).undeploy();
        }

        // TODO before or after application undeployment?
        // TODO not yet clear if this method should be invoked (or not) in presence of ephemeral namespaces,
        //  test failures, or retain on failure
        runPublicStaticVoidMethods(CustomizeApplicationUndeployment.class, context);

        dropEphemeralNamespaceIfNecessary(context);
    }

    private void dropEphemeralNamespaceIfNecessary(ExtensionContext context) throws IOException, InterruptedException {
        EphemeralNamespace ephemeralNamespace = getStore(context).get(EphemeralNamespace.class.getName(),
                EphemeralNamespace.class);
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

    private void runPublicStaticVoidMethods(Class<? extends Annotation> annotation, ExtensionContext context) throws Exception {
        for (Method method : context.getRequiredTestClass().getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                if (!isPublicStaticVoid(method)) {
                    throw new OpenShiftTestException("@" + annotation.getSimpleName()
                            + " method " + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                            + " must be public static void");
                }

                Parameter[] parameters = method.getParameters();
                Object[] arguments = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    InjectionPoint injectionPoint = InjectionPoint.forParameter(parameter);
                    arguments[i] = valueFor(injectionPoint, context);
                }

                method.invoke(null, arguments);
            }
        }
    }

    private static boolean isPublicStaticVoid(Method method) {
        return Modifier.isPublic(method.getModifiers())
                && Modifier.isStatic(method.getModifiers())
                && Void.TYPE.equals(method.getReturnType());
    }

    // ---

    @Override
    public void beforeEach(ExtensionContext context) {
        System.out.println(ansi().a("---------- running test ")
                .fgYellow().a(context.getParent().map(ctx -> ctx.getDisplayName() + ".").orElse(""))
                .a(context.getDisplayName()).reset().a(" ----------"));
    }

    // ---

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        injectDependencies(testInstance, context);
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
        } else if (KnativeClient.class.equals(injectionPoint.type())) {
            return getOpenShiftClient(context).adapt(KnativeClient.class);
        } else if (AppMetadata.class.equals(injectionPoint.type())) {
            return getAppMetadata(context);
        } else if (AwaitUtil.class.equals(injectionPoint.type())) {
            return getAwaitUtil(context);
        } else if (OpenShiftUtil.class.equals(injectionPoint.type())) {
            return getOpenShiftUtil(context);
        } else if (Config.class.equals(injectionPoint.type())) {
            return Config.get();
        } else if (URL.class.equals(injectionPoint.type())) {
            return getURL(injectionPoint, context);
        } else {
            throw new OpenShiftTestException("Unsupported type " + injectionPoint.type().getSimpleName()
                    + " for @TestResource " + injectionPoint.description());
        }
    }

    private Object getURL(InjectionPoint injectionPoint, ExtensionContext context) throws OpenShiftTestException {
        String address;
        if (injectionPoint.isAnnotationPresent(WithName.class)) {
            String routeName = injectionPoint.getAnnotation(WithName.class).value();
            address = getBaseAddress(getOpenShiftClient(context).routes().withName(routeName).get());
        } else {
            AppMetadata metadata = getAppMetadata(context);
            String routeName = metadata.appName;
            address = getBaseAddress(getOpenShiftClient(context).routes().withName(routeName).get());
            if (metadata.httpRoot != null && metadata.httpRoot.length() > 1) { // skip httpRoot "/" case
                address = address + metadata.httpRoot;
            }
        }
        try {
            return new URL(address);
        } catch (MalformedURLException e) {
            throw new OpenShiftTestException("Couldn't construct URL for " + injectionPoint.type().getSimpleName()
                    + " for @TestResource " + injectionPoint.description());
        }
    }

    private String getBaseAddress(Route route) {
        if (route.getSpec().getTls() != null) {
            return "https://" + route.getSpec().getHost();
        } else {
            return "http://" + route.getSpec().getHost();
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

    private void runOnFailureAction(ExtensionContext context, OnOpenShiftFailureAction action) {
        try {
            injectDependencies(action, context);
            action.execute();
        } catch (Exception ex) {
            System.out.println(ansi().a("Error running post failure action. Caused by: " + ex).reset());
        }
    }

    private DeploymentStrategy getDeploymentStrategy(ExtensionContext context) throws Exception {
        DeploymentStrategy strategy = getStore(context)
                .getOrComputeIfAbsent(DeploymentStrategy.class.getName(),
                        ignored -> OpenShiftDeploymentStrategyLoader.load(context), DeploymentStrategy.class);

        injectDependencies(strategy, context);

        return strategy;
    }

    private void injectDependencies(Object instance, ExtensionContext context) throws Exception {
        for (Field field : findAnnotatedFields(instance.getClass(), TestResource.class, ignored -> true)) {
            InjectionPoint injectionPoint = InjectionPoint.forField(field);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(instance, valueFor(injectionPoint, context));
        }
    }

    private void failureOccured(ExtensionContext context) {
        getTestsStatus(context).failed = true;
    }
}
