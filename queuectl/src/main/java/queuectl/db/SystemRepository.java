package queuectl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;


public class SystemRepository {
    /**
     * Returns a map of job counts by state, e.g. { "pending": 3, "completed": 2 }
     */
    public static Map<String, Integer> getJobCounts() throws SQLException {
        String sql = "SELECT state, COUNT(*) AS count FROM jobs GROUP BY state";
        Map<String, Integer> counts = new HashMap<>();

        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("state"), rs.getInt("count"));
            }
        }
        return counts;
    }

    /**
     * Returns total job count.
     */
    public static int getTotalJobs() throws SQLException {
        String sql = "SELECT COUNT(*) FROM jobs";
        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Returns number of active workers (optional: future enhancement).
     * For now, returns 0 since worker tracking not implemented yet.
     */
    public static int getActiveWorkers() {
        return 0; // placeholder for later worker management
    }
}
