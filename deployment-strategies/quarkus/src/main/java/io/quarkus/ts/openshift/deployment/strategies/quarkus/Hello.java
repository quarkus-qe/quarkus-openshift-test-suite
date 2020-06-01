package io.quarkus.ts.openshift.deployment.strategies.quarkus;

public class Hello {
    private final String content;

    public Hello(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}

