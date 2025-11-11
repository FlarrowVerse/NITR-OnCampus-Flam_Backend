package queuectl.cli;

import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import queuectl.db.DLQRepository;
import queuectl.model.Job;

@Command(
    name = "list",
    description = "View all jobs in Dead Letter Queue"
)
public class DLQListCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {

        List<Job> jobs = DLQRepository.listAll();

        System.out.printf("%-10s | %-20s | %-9s | %-11s | %-25s | %-40s%n",
                "ID", "COMMAND", "ATTEMPTS", "MAX_RETRIES", "FAILED_AT", "LAST_ERROR");
        System.out.println("------------------------------------------------------------------------------------------------------------------");

        if (jobs.isEmpty()) {
            System.out.println("(DLQ is empty)");
        } else {
            for (Job job : jobs) {
                System.out.printf("%-10s | %-20s | %-9d | %-11d | %-25s | %-40s%n",
                        job.id,
                        job.command,
                        job.attempts,
                        job.maxRetries,
                        job.updatedAt,
                        job.lastError != null ? job.lastError.substring(0, Math.min(job.lastError.length(), 40)) : "");
            }
        }
        return 0;
    }
}
