package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a {@code public static void} method is annotated {@code CustomizeApplicationDeployment}, it is executed after all
 * {@link AdditionalResources} are deployed, but before the application resources are deployed.
 * It gives the test a chance to modify the application deployment YAML file, for example.
 * If {@link ManualApplicationDeployment} is used, this method can be used to deploy a test application.
 * <p>
 * The method can have arbitrary parameters, but all of them must be injectable by the
 * {@link io.quarkus.ts.openshift.common.injection.TestResource @TestResource} mechanism.
 * The parameters <em>don't</em> have to be annotated {@code @TestResource}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomizeApplicationDeployment {
}
