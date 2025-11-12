package queuectl.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import queuectl.db.ConfigRepository;
import queuectl.db.Database;
import queuectl.db.WorkerRepository;
import queuectl.model.Job;

public class WorkerService implements Runnable {

    private final String workerId = UUID.randomUUID().toString();
    private int backoffBase, defaultMaxRetries, reclaimTimeout;
    private volatile boolean running = true;

    public WorkerService(int backoffBase) {
        this.backoffBase = ConfigRepository.getInt("backoff_base", 2);
        this.defaultMaxRetries = ConfigRepository.getInt("max_retries", 3);
        this.reclaimTimeout = ConfigRepository.getInt("reclaim_timeout", 30);
    }

    @Override
    public void run() {
        System.out.println("Worker " + workerId + " started.");

        long lastConfigReload = 0;
        while (running) {
            if (System.currentTimeMillis() - lastConfigReload > 60_000) {
                this.backoffBase = ConfigRepository.getInt("backoff_base", backoffBase);
                this.defaultMaxRetries = ConfigRepository.getInt("max_retries", defaultMaxRetries);
                
                lastConfigReload = System.currentTimeMillis();
                
                System.out.printf("[%s] 🔁 Reloaded config: backoffBase=%d, maxRetries=%d%n",
                    workerId.substring(0,5), backoffBase, defaultMaxRetries);
            }
            try (Connection conn = Database.get()) {
                conn.setAutoCommit(false);
                
                if (Math.random() < 0.1) { // occasionally run cleanup
                    int reclaimed = WorkerRepository.reclaimStaleJobs(conn, reclaimTimeout); // 30 sec timeout
                    if (reclaimed > 0) {
                        System.out.printf("[%s] ♻️ Reclaimed %d stuck jobs%n", workerId.substring(0,5), reclaimed);
                    }
                }
                
                Optional<Job> optJob = WorkerRepository.fetchNextPendingJob(conn, workerId);

                if (optJob.isEmpty()) {
                    conn.commit();
                    Thread.sleep(2000); // Idle sleep
                    continue;
                }

                Job job = optJob.get();
                conn.commit();

                boolean success = executeCommand(job.command);

                conn.setAutoCommit(false);
                if (success) {
                    WorkerRepository.markCompleted(conn, job.id);
                    conn.commit();
                    System.out.printf("[%s] ✅ Completed job %s%n", Instant.now(), job.id);
                } else {
                    WorkerRepository.markFailed(conn, job, backoffBase, defaultMaxRetries);
                    conn.commit();
                    System.out.printf("[%s] ❌ Failed job %s (attempt %d)%n",
                            Instant.now(), job.id, job.attempts + 1);
                }

            } catch (InterruptedException e) {
                running = false; // stop loop if interrupted
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Worker error: " + e.getMessage());
            }
        }
        System.out.println("Worker " + workerId + " exiting gracefully.");
    }

    private boolean executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + workerId.substring(0, 5) + "] " + line);
                }
            }
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Command error: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        running = false;
    }

}
