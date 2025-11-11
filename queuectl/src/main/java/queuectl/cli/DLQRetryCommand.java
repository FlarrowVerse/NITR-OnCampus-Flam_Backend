package queuectl.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import queuectl.db.DLQRepository;

@Command( name = "retry", description = "Retry a job from the Dead Letter Queue by Job ID")
public class DLQRetryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Job  ID to retry")
    private String jobId;

    @Override
    public Integer call() throws Exception {
        boolean success = DLQRepository.retry(jobId);

        if (success) {
            System.out.printf("Job '%s' moved back to main queue (state=pending).", jobId);
            return 0;
        } else {
            System.err.printf("No job found in DLQ with Job Id: %s", jobId);
            return 1;
        }
    }
}
