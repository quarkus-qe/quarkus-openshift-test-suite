package io.quarkus.ts.openshift.common;

import io.quarkus.ts.openshift.common.config.Config;

public class DefaultTimeout {
    static final String CONFIG_KEY = "ts.default-timeout";

    public static int getMinutes() {
        return Config.get().getAsInt(CONFIG_KEY, 10);
    }
}
