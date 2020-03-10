package io.quarkus.ts.openshift.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class Command {
    private final String description;
    private final List<String> command;

    public Command(String... command) {
        this(Arrays.asList(command));
    }

    public Command(List<String> command) {
        this.description = descriptionOfProgram(command.get(0));
        this.command = command;
    }

    private static String descriptionOfProgram(String program) {
        if (program.contains(File.separator)) {
            return program.substring(program.lastIndexOf(File.separator) + 1);
        }
        return program;
    }

    public void runAndWait() throws IOException, InterruptedException {
        System.out.println(ansi().a("running ").fgYellow().a(String.join(" ", command)).reset());

        Process process = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(command)
                .directory(new File(".").getAbsoluteFile())
                .start();

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(ansi().fgCyan().a(description).reset().a("> ").a(line));
                }
            } catch (IOException ignored) {
            }
        }, "stdout consumer for command " + description).start();

        int result = process.waitFor();
        if (result != 0) {
            throw new RuntimeException(description + " failed (return code " + result + ")");
        }
    }
}
