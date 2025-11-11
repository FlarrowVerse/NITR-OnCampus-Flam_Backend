package queuectl.cli;

import queuectl.db.JobRepository;
import queuectl.model.Job;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "list",
    description = "List jobs in the queue (optionally filtered by state)"
)
public class ListCommand implements Callable<Integer> {

    @Option(
        names = {"-s", "--state"},
        description = "Filter jobs by state (pending, processing, completed, failed, dead)"
    )
    private String state;

    @Override
    public Integer call() throws Exception {
        List<Job> jobs = JobRepository.list(state);

        System.out.printf("%-10s | %-20s | %-10s | %-8s | %-11s | %-20s | %-20s%n",
                "ID", "COMMAND", "STATE", "ATTEMPTS", "MAX_RETRIES", "CREATED_AT", "UPDATED_AT");
        System.out.println("------------------------------------------------------------------------------------------------------------");

        if (jobs.isEmpty()) {
            System.out.println("No jobs found");
        } else {
            for (Job job : jobs) {
                System.out.printf("%-10s | %-20s | %-10s | %-8d | %-11d | %-20s | %-20s%n",
                    job.id, job.command, job.state, job.attempts, job.maxRetries, job.createdAt, job.updatedAt);
            }
        }

        return 0;

    }
    
}
