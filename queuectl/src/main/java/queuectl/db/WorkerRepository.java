package queuectl.db;

import queuectl.model.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;

public class WorkerRepository {
    
    // Atomically fetch one pending job and mark as processing
    public static Optional<Job> fetchNextPendingJob(Connection conn, String workerId) throws SQLException {
        String sql = """
            UPDATE jobs
            SET state = 'processing', worker_id = ?, updated_at = NOW()
            WHERE id = (
                SELECT id FROM jobs
                WHERE state = 'pending' OR state = 'failed' AND next_run_at <= NOW()
                ORDER BY created_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, command, attempts, max_retries
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Job job = new Job();
                job.id = rs.getString("id");
                job.command = rs.getString("command");
                job.attempts = rs.getInt("attempts");
                job.maxRetries = rs.getInt("max_retries");
                return Optional.of(job);
            }
        }
        return Optional.empty();
    }

    // Mark job completed
    public static void markCompleted(Connection conn, String jobId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE jobs SET state='completed', updated_at=NOW() WHERE id=?")) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        }
    }

    // Mark job failed and schedule retry/backoff
    public static void markFailed(Connection conn, Job job, int backoffBase) throws SQLException {
        job.attempts++;
        boolean retry = job.attempts < job.maxRetries;

        if (retry) {
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE jobs
                SET state='failed',
                    attempts=?,
                    next_run_at=NOW() + (? ^ ?) * INTERVAL '1 second',
                    updated_at=NOW()
                WHERE id=?""")) {
                ps.setInt(1, job.attempts);
                ps.setInt(2, backoffBase);
                ps.setInt(3, job.attempts);
                ps.setString(4, job.id);
                ps.executeUpdate();
            }
        } else {
            // Move to DLQ
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO dlq (id, command, attempts, max_retries, failed_at, last_error)
                SELECT id, command, attempts, max_retries, NOW(), 'max retries reached'
                FROM jobs WHERE id=? ON CONFLICT (id) DO NOTHING
            """)) {
                ps.setString(1, job.id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM jobs WHERE id=?")) {
                ps.setString(1, job.id);
                ps.executeUpdate();
            }
        }
    }

    public static int reclaimStaleJobs(Connection conn, int timeoutSeconds) throws SQLException {
    String sql = """
        UPDATE jobs
        SET state='pending', worker_id=NULL, updated_at=NOW()
        WHERE state='processing' AND updated_at < NOW() - (? * INTERVAL '1 second')
        RETURNING id
    """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, timeoutSeconds);
        ResultSet rs = ps.executeQuery();
        int count = 0;
        while (rs.next()) count++;
        return count;
    }
}

}
