package queuectl.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "queuectl",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    subcommands = { EnqueueCommand.class, ListCommand.class, WorkerCommand.class, StatusCommand.class, DLQCommand.class, ConfigCommand.class },
    description = "QueueCTL CLI entrypoint"
)
public class QueueCtl implements Runnable {

    @Override
    public void run() {
        System.out.println("QueueCTL ready");
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new QueueCtl()).execute(args);
        System.exit(exit);
    }
}
