package queuectl;

import queuectl.cli.QueueCtl;
import queuectl.db.Database;
import org.flywaydb.core.Flyway;

import picocli.CommandLine;

import java.io.InputStream;
import java.util.Properties;

public class App {
    public static void main(String[] args) {
        Properties p = new Properties();
        try (InputStream in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) p.load(in);
        } catch (Exception e) {
            System.err.println("Config load error: " + e.getMessage());
        }

        Database.init(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"));

        // ------------------------------------------------------------------
        // Control migration with environment variable or CLI flag
        // ------------------------------------------------------------------
        boolean runMigrations = Boolean.parseBoolean(System.getenv().getOrDefault("QUEUECTL_MIGRATE", "false"));
        if (runMigrations) {
            try {
                Class.forName("org.postgresql.Driver");
                Flyway flyway = Flyway.configure()
                        .dataSource(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"))
                        .load();
                flyway.migrate();
                System.out.println("Flyway migrations executed successfully.");
            } catch (Exception e) {
                System.err.println("Flyway migration failed: " + e.getMessage());
            }
        } else {
            System.out.println("Skipping Flyway migration (set QUEUECTL_MIGRATE=true to enable).");
        }
        // ------------------------------------------------------------------



        int exit = new CommandLine(new QueueCtl()).execute(args);
        Database.close();
        System.exit(exit);
    }
}
