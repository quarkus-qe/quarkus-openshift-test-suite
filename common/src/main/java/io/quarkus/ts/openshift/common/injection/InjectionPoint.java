package io.quarkus.ts.openshift.common.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public interface InjectionPoint {
    Class<?> type();

    String description();

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    static InjectionPoint forField(Field field) {
        return new FieldInjectionPoint(field);
    }

    static InjectionPoint forParameter(Parameter parameter) {
        return new ParameterInjectionPoint(parameter);
    }
}
