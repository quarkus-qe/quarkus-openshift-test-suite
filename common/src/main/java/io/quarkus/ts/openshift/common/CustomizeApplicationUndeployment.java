package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a {@code public static void} method is annotated {@code CustomizeApplicationUndeployment}, it is executed after
 * the application resources are undeployed, but before all {@link AdditionalResources} are undeployed.
 * It gives the test a chance to undeploy resources deployed by {@link CustomizeApplicationDeployment}, for example.
 * <p>
 * The method can have arbitrary parameters, but all of them must be injectable by the
 * {@link io.quarkus.ts.openshift.common.injection.TestResource @TestResource} mechanism.
 * The parameters <em>don't</em> have to be annotated {@code @TestResource}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomizeApplicationUndeployment {
}
