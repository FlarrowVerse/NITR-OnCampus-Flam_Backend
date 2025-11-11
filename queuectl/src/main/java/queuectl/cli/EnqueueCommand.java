package queuectl.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import queuectl.db.JobRepository;
import queuectl.model.Job;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "enqueue", description = "Add a new job to the queue")
public class EnqueueCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Job JSON string(Optional), if omitted, reads from STDIN", arity="0..1")
    private String jobJson;

    @Override
    public Integer call() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        if (jobJson == null || jobJson.isBlank()) {
            System.out.println("Reading JSON from STDIN... (Press Ctrl+D or Ctrl+Z when done)");
            jobJson = new BufferedReader(new InputStreamReader(System.in)).lines().collect(Collectors.joining("\n"));
        }
        try {
            Job job = mapper.readValue(jobJson, Job.class);
            try {
                JobRepository.insert(job);
                System.out.println("Enqueued job: " + job);
                return 0;
            } catch (SQLException e) {
                if (e.getSQLState().equals("23505")) {
                    System.err.println("Job with id '" + job.id + "' already exists.");
                } else {
                    System.err.println("DB error: " + e.getMessage());
                }
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Invalid JSON: " + e.getMessage());
            return 2;
        }
    }
}