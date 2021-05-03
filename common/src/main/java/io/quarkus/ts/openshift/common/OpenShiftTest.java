package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.ts.openshift.common.deploy.DeploymentStrategy;
import io.quarkus.ts.openshift.common.deploy.EmbeddedDeploymentStrategy;

/**
 * Marks the test class as OpenShift test.
 * That is, the JUnit extension for testing Quarkus applications on OpenShift will be applied.
 * <p>
 * For running the tests, you need to be logged into OpenShift ({@code oc login ...}).
 * <p>
 * The test framework requires that the {@code app-metadata} Quarkus extension is present, because
 * it heavily relies on the informations the extension collects into {@code target/app-metadata.properties}.
 * <p>
 * The {@code oc} binary needs to be present on {@code PATH}, as the test framework uses it.
 * <p>
 * Before running the tests in this class, the test application is deployed. It is expected that
 * the resources to be deployed are found in {@code target/kubernetes/openshift.yml}.
 * After the tests in this class finish, the application is undeployed.
 * <p>
 * The {@link io.quarkus.ts.openshift.common.injection.TestResource @TestResource} annotation
 * can be used to inject some useful objects. The annotation can be present on a field, or
 * on a test method parameter.
 * <p>
 * The {@link AdditionalResources @AdditionalResources} annotation can be used to deploy
 * additional OpenShift resources before the test application is deployed. These resources
 * are automatically undeployed after the test application is undeployed.
 * <p>
 * The {@link OnlyIfConfigured @OnlyIfConfigured} annotation can be used to selectively
 * enable/disable execution of tests based on a configuration property.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OpenShiftTestExtension.class)
public @interface OpenShiftTest {
    /**
     * Specify the strategy to deploy the OpenShift resources. The available options are:
     * - EmbeddedDeploymentStrategy (default): via the test framework.
     * - UsingQuarkusPluginDeploymentStrategy: via the Quarkus plugin.
     * - ManualDeploymentStrategy: will do nothing as the test case should manage the resources manually.
     */
    Class<? extends DeploymentStrategy> strategy() default EmbeddedDeploymentStrategy.class;
}
