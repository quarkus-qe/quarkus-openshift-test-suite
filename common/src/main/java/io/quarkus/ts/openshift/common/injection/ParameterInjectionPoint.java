package io.quarkus.ts.openshift.common.injection;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

final class ParameterInjectionPoint implements InjectionPoint {
    private final Parameter parameter;

    public ParameterInjectionPoint(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public Class<?> type() {
        return parameter.getType();
    }

    @Override
    public String description() {
        Executable method = parameter.getDeclaringExecutable();
        return "parameter " + parameter.getName() + " in " + method.getDeclaringClass().getSimpleName()
                + "." + method.getName();
    }
}
