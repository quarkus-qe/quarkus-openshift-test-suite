package io.quarkus.ts.openshift.common;

import io.quarkus.ts.openshift.common.config.Config;

final class RetainOnFailure {
    static final String CONFIG_KEY = "ts.retain-on-failure";

    static boolean isEnabled() {
        return Config.get().getAsBoolean(CONFIG_KEY, false);
    }
}
