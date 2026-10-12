package com.burcuduzen.dayflow.desktop;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SchemaMigrationTests {
    @Test void legacyHistoryIsPreservedAndMigrationIsRepeatable() throws Exception {
        String url = "jdbc:h2:mem:migration-test;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            String schema = new String(getClass().getResourceAsStream("/db/migration/V1__initial_schema.sql").readAllBytes(), StandardCharsets.UTF_8);
            schema = schema.replace("estimated_minutes integer, completed_at", "completed_at");
            for (String sql : schema.split(";")) if (!sql.isBlank()) statement.execute(sql);
            statement.execute("ALTER TABLE user_settings ADD focus_minutes INTEGER NOT NULL DEFAULT 25");
            statement.execute("ALTER TABLE user_settings ALTER COLUMN focus_minutes DROP DEFAULT");
            statement.execute("INSERT INTO tasks (id,title,priority,status,created_at,updated_at,estimated_minutes) VALUES (1,'Legacy','MEDIUM','COMPLETED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,35)");
            statement.execute("INSERT INTO completion_activity (task_id,completed_at) VALUES (1,CURRENT_TIMESTAMP)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true).baselineVersion("1").load();
        flyway.migrate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM tasks WHERE id=1");
            try (var rows = statement.executeQuery("SELECT estimated_minutes FROM completion_activity WHERE task_id=1")) {
                assertTrue(rows.next()); assertEquals(35, rows.getInt(1));
            }
            statement.execute("INSERT INTO user_settings (id,daily_task_goal,work_start,work_end,updated_at,time_zone) VALUES (1,3,'09:00:00','18:00:00',CURRENT_TIMESTAMP,'Europe/Istanbul')");
        }
    }
}
