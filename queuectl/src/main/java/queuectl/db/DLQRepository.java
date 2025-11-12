package queuectl.db;

import queuectl.model.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class DLQRepository {
    public static List<Job> listAll() throws SQLException {
        List<Job> jobs = new ArrayList<>();

        String sql = """
            SELECT id, command, attempts, max_retries, failed_at, last_error 
            FROM dlq
            ORDER BY failed_at DESC
        """;

        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Job job = new Job();
                    job.id = rs.getString("id");
                    job.command = rs.getString("command");
                    job.attempts = rs.getInt("attempts");
                    job.maxRetries = rs.getInt("max_retries");
                    job.lastError = rs.getString("last_error");
                    job.updatedAt = rs.getObject("failed_at", OffsetDateTime.class);
                }
            
        }
        return jobs;
    }

    public static boolean retry(String jobId) throws SQLException {
        try (Connection conn = Database.get()) {
            conn.setAutoCommit(false);

            // fetch job from DLQ
            String selectSQL = """
                SELECT * FROM dlq WHERE id=?
            """;

            Job job = null;

            try (PreparedStatement ps = conn.prepareStatement(selectSQL)) {
                ps.setString(1, jobId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    job = new Job();
                    job.id = rs.getString("id");
                    job.command = rs.getString("command");
                    job.attempts = 0;
                    job.maxRetries = rs.getInt("max_retries");
                }
            }

            if (job == null) {
                conn.rollback();
                return false;
            }

            // Reinsert into main jobs queue
            String insertSql = """
                INSERT INTO jobs (id, command, state, attempts, max_retries, created_at, updated_at, next_run_at)
                VALUES (?, ?, 'pending'::job_state, 0, ?, NOW(), NOW(), NOW())
                ON CONFLICT (id) DO UPDATE SET state='pending', attempts=0, updated_at=NOW(), next_run_at=NOW()
            """;

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, job.id);
                ps.setString(2, job.command);
                ps.setInt(3, job.maxRetries);
                ps.executeUpdate();
            }

            // Remove from DLQ
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dlq WHERE id = ?")) {
                ps.setString(1, job.id);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        }
    }
}
