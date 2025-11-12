package queuectl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigRepository {
    public static String getString(String key, String defaultValue) throws SQLException {
        String sql = "SELECT value FROM config WHERE key = ?";
        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String s = getString(key, Integer.toString(defaultValue));
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void set(String key, String value) throws SQLException {
        String sql = "INSERT INTO config(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = EXCLUDED.value";
        try (Connection conn = Database.get();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
