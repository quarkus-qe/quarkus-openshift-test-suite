package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If the test class is annotated {@code ManualApplicationDeployment}, tested application will not be deployed
 * automatically and it is expected that it will be deployed using {@link CustomizeApplicationDeployment} and
 * {@link AdditionalResources}. If {@code CustomizeApplicationDeployment} is used to deploy the application,
 * then {@link CustomizeApplicationUndeployment} should be used to undeploy it.
 *
 * @see CustomAppMetadata
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManualApplicationDeployment {
}
