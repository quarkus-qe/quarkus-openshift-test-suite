package io.quarkus.ts.openshift.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a {@code public static void} method is annotated {@code BeforeApplicationDeployment}, it is executed after all
 * {@link AdditionalResources} are processed, but before the application resources are deployed.
 * It gives the test a chance to modify the application deployment YAML file, for example.
 * <p>
 * The method can have arbitrary parameters, but all of them must be injectable by the
 * {@link io.quarkus.ts.openshift.common.injection.TestResource @TestResource} mechanism.
 * The parameters <em>don't</em> have to be annotated {@code @TestResource}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeApplicationDeployment {
    // TODO this is not fully thought through and should be considered very much experimental
    //      for the same reason, it isn't documented in the README
}
