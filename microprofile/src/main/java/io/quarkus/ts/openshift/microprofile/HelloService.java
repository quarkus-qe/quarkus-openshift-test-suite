package io.quarkus.ts.openshift.microprofile;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.opentracing.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HelloService {
    @Inject
    ManagedExecutor executor;

    @Inject
    Tracer tracer;

    public CompletionStage<String> get(String name) {
        tracer.activeSpan().log("HelloService called");
        return executor.supplyAsync(() -> {
            tracer.activeSpan().log("HelloService async processing");
            return "Hello, " + name + "!";
        });
    }
}
