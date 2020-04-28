package io.quarkus.ts.openshift.microprofile;

import io.opentracing.Tracer;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class HelloService {
    @Inject
    ManagedExecutor executor;

    @Inject
    Tracer tracer;

    private volatile boolean enabled = true;

    @Asynchronous
    public CompletionStage<String> get(String name) {
        if (!enabled) {
            tracer.activeSpan().log("HelloService disabled");
            CompletableFuture<String> result = new CompletableFuture<>();
            result.completeExceptionally(new BadRequestException("HelloService disabled"));
            return result;
        }

        tracer.activeSpan().log("HelloService called");

        return executor.supplyAsync(() -> {
            tracer.activeSpan().log("HelloService async processing");
            return "Hello, " + name + "!";
        });
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }
}
