package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If the test class is annotated {@code CustomAppMetadata}, then the annotation is used as a source
 * of {@link io.quarkus.ts.openshift.app.metadata.AppMetadata} for the test. This must be used when deploying
 * an application that lives outside of the test suite and can't use the {@code app-metadata} Quarkus extension.
 *
 * @see ManualApplicationDeployment
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomAppMetadata {
    /**
     * Name of the application, which is also used as a name of the Kubernetes resources.
     * Corresponds to {@code quarkus.application.name} and/or {@code quarkus.container-image.name}.
     * @see io.quarkus.ts.openshift.app.metadata.AppMetadata#appName
     */
    String appName();

    /**
     * Root path for the HTTP endpoint of the application.
     * Corresponds to {@code quarkus.http.root-path}.
     * @see io.quarkus.ts.openshift.app.metadata.AppMetadata#httpRoot
     */
    String httpRoot();

    /**
     * Known endpoint that can be used to find out if the application is already running.
     * If the application has a readiness or liveness probe, it can be used here.
     * @see io.quarkus.ts.openshift.app.metadata.AppMetadata#knownEndpoint
     */
    String knownEndpoint();

    /**
     * The target deployment platform.
     * @see io.quarkus.ts.openshift.app.metadata.AppMetadata#deploymentTarget
     */
    String deploymentTarget() default "";
}
