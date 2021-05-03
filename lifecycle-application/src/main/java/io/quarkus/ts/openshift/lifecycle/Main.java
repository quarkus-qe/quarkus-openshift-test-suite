package io.quarkus.ts.openshift.lifecycle;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {

    public static final String ARGUMENTS_FROM_MAIN = "Received arguments: ";

    private static final Logger LOG = Logger.getLogger(Main.class);

    public static void main(String... args) {
        LOG.info(ARGUMENTS_FROM_MAIN + String.join(",", args));
        Quarkus.run(args);
    }
}
