package queuectl.cli;

import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import queuectl.db.SystemRepository;

@Command(name = "status", description = "Show summary of all job states & active workers")
public class StatusCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        Map<String, Integer> jobCounts = SystemRepository.getJobCounts();
        int total = SystemRepository.getTotalJobs();
        int workers = SystemRepository.getActiveWorkers();

        System.out.println("QueueCTL System Status");
        System.out.println("────────────────────────────────────────────");
        System.out.printf("%-15s : %d%n", "Total Jobs", total);
        System.out.println();

        for (String state : new String[]{"pending", "processing", "completed", "failed", "dead"}) {
            System.out.printf("%-15s : %d%n", capitalize(state), jobCounts.getOrDefault(state, 0));
        }

        System.out.println("────────────────────────────────────────────");
        System.out.printf("%-15s : %d%n", "Active Workers", workers);

        return 0;
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
