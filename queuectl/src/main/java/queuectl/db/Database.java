package queuectl.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Centralized database connection manager using HikariCP.
 * 
 * - In production: call init() once with values from application.properties
 * - In tests: call overrideUrl() to connect to a Testcontainers or H2 DB
 */
public class Database {

    private static HikariDataSource ds;
    private static String activeUrl;
    private static String activeUser;
    private static String activePass;

    /**
     * Initialize connection pool (used in production).
     * Safe to call multiple times; will only initialize once.
     */
    public static synchronized void init(String url, String user, String pass) {
        if (ds != null) return;

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5);
        cfg.setAutoCommit(true);
        cfg.setPoolName("QueueCTLPool");

        ds = new HikariDataSource(cfg);
        activeUrl = url;
        activeUser = user;
        activePass = pass;

        System.out.println("✅ Database initialized at " + url);
    }

    /**
     * Override database connection for testing (e.g., Testcontainers or H2).
     * This will close the existing pool and create a new one.
     */
    public static synchronized void overrideUrl(String url, String user, String pass) {
        if (ds != null) {
            try {
                ds.close();
                System.out.println("♻️ Closed existing Hikari pool before override.");
            } catch (Exception ignored) {}
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(3);
        cfg.setAutoCommit(true);
        cfg.setPoolName("QueueCTLTestPool");

        ds = new HikariDataSource(cfg);
        activeUrl = url;
        activeUser = user;
        activePass = pass;

        System.out.println("⚙️ Overridden DB connection for testing: " + url);
    }

    /**
     * Obtain a DB connection.
     */
    public static Connection get() throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("❌ Database not initialized. Call init() or overrideUrl() first.");
        }
        return ds.getConnection();
    }

    /**
     * Shutdown the pool (used on shutdown or after tests).
     */
    public static synchronized void close() {
        if (ds != null) {
            ds.close();
            ds = null;
            System.out.println("🧹 Hikari pool closed.");
        }
    }

    // Optional getters for debugging or test assertions
    public static String getActiveUrl() { return activeUrl; }
    public static String getActiveUser() { return activeUser; }
}
