package io.quarkus.ts.openshift.common;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code @OnlyIfNotConfigured} annotation can be used to selectively enable or disable certain tests
 * based on a configuration property (using the built-in {@linkplain io.quarkus.ts.openshift.common.config.Config configuration
 * system}).
 * The test is only executed if the specified configuration key evaluates to {@code false}.
 *
 * @see #value()
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OnlyIfNotConfiguredCondition.class)
public @interface OnlyIfNotConfigured {
    /**
     * Configuration key that must be disabled (per
     * {@link io.quarkus.ts.openshift.common.config.Config#getAsBoolean(String, boolean) Config.getAsBoolean})
     * for the annotated test to be executed.
     */
    String value();
}