package io.quarkus.ts.openshift.common.deploy;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.ts.openshift.common.OpenShiftTest;

public final class OpenShiftDeploymentStrategyLoader {

    private OpenShiftDeploymentStrategyLoader() {

    }

    public static final DeploymentStrategy load(ExtensionContext context) {
        try {
            Class<? extends DeploymentStrategy> strategyClazz = context.getElement()
                    .map(it -> it.getAnnotation(OpenShiftTest.class).strategy()).get();
            return strategyClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error loading OpenShift deployment strategy", e);
        }
    }
}
