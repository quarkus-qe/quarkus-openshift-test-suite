package io.quarkus.ts.openshift.common;

import io.quarkus.ts.openshift.common.config.Config;

import java.util.concurrent.ThreadLocalRandom;

final class EphemeralNamespace {
    static final String CONFIG_KEY = "ts.use-ephemeral-namespaces";

    static boolean isEnabled() {
        return Config.get().getAsBoolean(CONFIG_KEY, false);
    }

    static boolean isDisabled() {
        return !isEnabled();
    }

    static EphemeralNamespace newWithRandomName() {
        String name = ThreadLocalRandom.current()
                .ints(10, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("ts-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return new EphemeralNamespace(name);
    }

    final String name;

    private EphemeralNamespace(String name) {
        this.name = name;
    }
}
