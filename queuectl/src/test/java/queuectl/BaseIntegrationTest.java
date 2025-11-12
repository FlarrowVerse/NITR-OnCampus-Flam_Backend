package queuectl;

import queuectl.db.Database;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;

public abstract class BaseIntegrationTest {

    @BeforeAll
    static void setupDatabase() throws Exception {
        // Use H2 in PostgreSQL compatibility mode
        String url = "jdbc:h2:mem:queuectl;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String pass = "";

        // Override your Database connection for tests
        Database.overrideUrl(url, user, pass);

        // Apply Flyway migrations on H2 memory DB
        Flyway.configure()
              .dataSource(url, user, pass)
              .locations("classpath:db/migration")
              .load()
              .migrate();

        try (Connection conn = Database.get()) {
            System.out.println("✅ H2 Test Database ready: " + conn.getMetaData().getURL());
        }
    }

    @AfterAll
    static void tearDown() {
        Database.close();
    }
}
