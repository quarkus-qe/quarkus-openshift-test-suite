package io.quarkus.ts.openshift.common;

import io.quarkus.ts.openshift.common.config.Config;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class OnlyIfConfiguredCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = ConditionEvaluationResult.enabled(
            "@OnlyIfConfigured is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<OnlyIfConfigured> onlyIfConfigured = findAnnotation(element, OnlyIfConfigured.class);
        if (onlyIfConfigured.isPresent()) {
            String key = onlyIfConfigured.get().value();
            return Config.get().getAsBoolean(key, false)
                    ? ConditionEvaluationResult.enabled("Configuration property " + key + " is enabled")
                    : ConditionEvaluationResult.disabled("Configuration property " + key + " is disabled");
        }
        return ENABLED_BY_DEFAULT;
    }
}
