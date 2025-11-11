package queuectl.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static HikariDataSource ds;

    public static void init(String url, String user, String pass) {
        if (ds != null) return;
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5);
        ds = new HikariDataSource(cfg);
    }

    public static Connection get() throws SQLException {
        if (ds == null) throw new IllegalStateException("DB not initialized");
        return ds.getConnection();
    }

    public static void close() { if (ds != null) ds.close(); }
}