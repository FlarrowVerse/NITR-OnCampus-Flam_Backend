package queuectl.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import queuectl.service.WorkerService;

@Command(name = "worker", description = "Start one or more workers", subcommands = {})
public class WorkerCommand implements Callable<Integer> {
    
    @Option(names = {"start"}, description = "Start worker threads", required = false)
    boolean startWorkers;

    @Option(names = {"--count"}, description = "Number of workers to be started(default=1)")
    int count = 1;

    @Option(names = {"--backoff-base"}, description = "Exponential backoff base(default=2)")
    int backoffBase = 2;

    @Override
    public Integer call() throws Exception {
        if (!startWorkers) {
            System.out.println("Usage: queuectl worker start --count <n>");
            return 0;
        }

        List<WorkerService> workers = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            WorkerService worker = new WorkerService(backoffBase);
            Thread thread = new Thread(worker, "worker-" + i);
            thread.start();
            workers.add(worker);
            threads.add(thread);
        }

        System.out.printf("Started %d workers. Press Ctrl+C to stop.%n", count);

        // ✅ Register shutdown hook for graceful stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown signal received. Stopping workers gracefully...");
            for (WorkerService worker : workers) {
                worker.stop();
            }
        }));

        // Keep main thread alive, but responsive
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted. Exiting...");
        }

        // Wait for all threads to finish their current work
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("✅ All workers stopped cleanly.");
        return 0;
    }
}
