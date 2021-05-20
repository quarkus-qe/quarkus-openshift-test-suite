package io.quarkus.ts.openshift.common.util;

public final class OsUtils {
    private OsUtils() {

    }

    public static boolean isWindows() {
        return System.getProperty("os.name").matches(".*[Ww]indows.*");
    }
}
