package queuectl.db;

import queuectl.model.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JobRepository {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static void insert(Job job) throws SQLException {
        String sql = """
            INSERT INTO jobs (id, command, state, attempts, max_retries, created_at, updated_at, next_run_at, last_error, worker_id)
            VALUES (?, ?, ?::job_state, ?, ?, ?::timestamp, ?::timestamp, ?::timestamp, ?, ?)
        """;
        try (Connection conn = Database.get(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, job.id);
            ps.setString(2, job.command);
            ps.setString(3, job.state);
            ps.setInt(4, job.attempts);
            ps.setInt(5, job.maxRetries);
            ps.setString(6, fmt.format(job.createdAt));
            ps.setString(7, fmt.format(job.updatedAt));
            ps.setString(8, fmt.format(job.nextRunAt));
            ps.setString(9, job.lastError);
            ps.setString(10, job.workerId);

            ps.executeUpdate();
        }
    }

    public static List<Job> list(String state) throws SQLException {
        List<Job> jobs = new ArrayList<>();

        String sql = """
            SELECT id, command, state, attempts, max_retries, created_at, updated_at, next_run_at, last_error, worker_id
            FROM jobs
        """;

        if (state != null && !state.isBlank()) {
            sql += " WHERE state = ?::job_state";
        }
        sql += " ORDER BY created_at DESC";

        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            if (state != null && !state.isBlank()) {
                ps.setString(1, state);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Job job = new Job();
                job.id = rs.getString("id");
                job.command = rs.getString("command");
                job.state = rs.getString("state");
                job.attempts = rs.getInt("attempts");
                job.maxRetries = rs.getInt("max_retries");
                job.createdAt = rs.getObject("created_at", OffsetDateTime.class);
                job.updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
                job.nextRunAt = rs.getObject("next_run_at", OffsetDateTime.class);
                job.lastError = rs.getString("last_error");
                job.workerId = rs.getString("worker_id");
                jobs.add(job);
            }
        }
        return jobs;
    }
}
