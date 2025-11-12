package queuectl;

import queuectl.db.Database;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest extends BaseIntegrationTest {

    @Test
    void testDatabaseConnection() throws Exception {
        try (Connection conn = Database.get()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT 1");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void testFlywayMigrationsApplied() throws Exception {
        try (Connection conn = Database.get()) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT table_name FROM information_schema.tables WHERE LOWER(table_name) = 'jobs'");
            assertTrue(rs.next(), "Expected 'jobs' table to exist after migration");
        }
    }

}
