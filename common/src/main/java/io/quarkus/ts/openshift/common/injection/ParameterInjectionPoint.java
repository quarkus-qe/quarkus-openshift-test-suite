package io.quarkus.ts.openshift.common.injection;

import java.lang.annotation.Annotation;
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

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return parameter.isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return parameter.getAnnotation(annotationClass);
    }
}
