package io.quarkus.ts.openshift.common.injection;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

public interface InjectionPoint {
    Class<?> type();

    String description();

    static InjectionPoint forField(Field field) {
        return new FieldInjectionPoint(field);
    }

    static InjectionPoint forParameter(Parameter parameter) {
        return new ParameterInjectionPoint(parameter);
    }
}
