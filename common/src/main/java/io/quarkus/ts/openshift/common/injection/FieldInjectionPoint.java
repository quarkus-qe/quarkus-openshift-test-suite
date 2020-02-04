package io.quarkus.ts.openshift.common.injection;

import java.lang.reflect.Field;

final class FieldInjectionPoint implements InjectionPoint {
    private final Field field;

    public FieldInjectionPoint(Field field) {
        this.field = field;
    }

    @Override
    public Class<?> type() {
        return field.getType();
    }

    @Override
    public String description() {
        return "field " + field.getDeclaringClass().getSimpleName() + "." + field.getName()
                + " of type " + field.getType().getSimpleName();
    }
}
