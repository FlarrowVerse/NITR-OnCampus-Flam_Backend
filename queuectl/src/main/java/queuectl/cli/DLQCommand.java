package queuectl.cli;

import picocli.CommandLine.Command;

@Command(
    name = "dlq", 
    description = "Manage the Dead Letter Queue (DLQ)",
    subcommands = { DLQListCommand.class, DLQRetryCommand.class }
)
public class DLQCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use subcommands: list | retry <jobId>");
    }
}
