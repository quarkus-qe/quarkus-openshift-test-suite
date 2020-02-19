package io.quarkus.ts.openshift.common.config;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple configuration mechanism for the test suite.
 * It currently only reads system properties, and only those that have a prefix of {@code ts.}.
 * In the future, it could possibly read other sources of data.
 */
public class Config {
    private static final String PREFIX = "ts.";

    private static final Config INSTANCE = new Config();

    private final Map<String, String> data;

    public static Config get() {
        return INSTANCE;
    }

    private Config() {
        this.data = System.getProperties()
                .entrySet()
                .stream()
                .filter(entry -> String.valueOf(entry.getKey()).startsWith(PREFIX))
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue())));
    }

    /**
     * Returns a boolean config value under given {@code key}.
     * The empty string and the {@code "true"} (case <i>insensitive</i>) string are considered truthy; all other strings are falsy.
     * If there's no value under the {@code key}, the {@code defaultValue} is returned instead.
     * Note that the {@code key} must begin with {@code ts.}, the prefix is not handled automatically.
     */
    public boolean getAsBoolean(String key, boolean defaultValue) {
        if (data.containsKey(key)) {
            String value = data.get(key);
            // `-Dts.my-config-key` means enabled
            return "".equals(value) || "true".equalsIgnoreCase(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns an integer config value under given {@code key}.
     * The {@link Integer#parseInt(String)} is used for conversion from string to int.
     * If there's no value under the {@code key}, the {@code defaultValue} is returned instead.
     * Note that the {@code key} must begin with {@code ts.}, the prefix is not handled automatically.
     */
    public int getAsInt(String key, int defaultValue) {
        if (data.containsKey(key)) {
            String value = data.get(key);
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns a string config value under given {@code key}.
     * If there's no value under the {@code key}, the {@code defaultValue} is returned instead.
     * Note that the {@code key} must begin with {@code ts.}, the prefix is not handled automatically.
     */
    public String getAsString(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
}
