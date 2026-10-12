package com.burcuduzen.dayflow.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class DesktopFilesTests {
    @TempDir Path directory;

    @Test void migrationRefusesALiveDatabaseThenCopiesItAfterClosing() throws Exception {
        Path legacy = directory.resolve("legacy");
        Path target = directory.resolve("target");
        Files.createDirectories(legacy);
        try (var connection = DriverManager.getConnection("jdbc:h2:file:" + legacy.resolve("dayflowj"), "sa", "")) {
            connection.createStatement().execute("CREATE TABLE tasks(id BIGINT PRIMARY KEY)");
            assertThrows(java.io.IOException.class, () -> DesktopFiles.prepare(target.toString(), legacy.toString()));
            assertFalse(Files.exists(target.resolve("dayflowj.mv.db")));
        }
        DesktopFiles.prepare(target.toString(), legacy.toString());
        assertTrue(Files.exists(target.resolve("dayflowj.mv.db")));
        assertTrue(Files.exists(legacy.resolve("dayflowj.mv.db")));
    }

    @Test void invalidArchiveCannotOverwriteExistingDatabase() throws Exception {
        Path database = directory.resolve("dayflowj.mv.db");
        Files.writeString(database, "existing-data");
        Path archive = directory.resolve("invalid.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../outside.mv.db"));
            zip.write("invalid".getBytes());
            zip.closeEntry();
        }
        assertThrows(Exception.class, () -> DesktopFiles.restore(archive, database));
        assertEquals("existing-data", Files.readString(database));
        assertFalse(Files.exists(directory.getParent().resolve("outside.mv.db")));
    }

    @Test void validBackupRestoresAndKeepsThePreviousDatabase() throws Exception {
        Path source = directory.resolve("source");
        Path archive = directory.resolve("valid.zip");
        try (var connection = DriverManager.getConnection("jdbc:h2:file:" + source, "sa", "")) {
            var statement = connection.createStatement();
            statement.execute("CREATE TABLE tasks(id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO tasks VALUES (7)");
        }
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("dayflowj.mv.db"));
            Files.copy(directory.resolve("source.mv.db"), zip);
            zip.closeEntry();
        }
        Path database = directory.resolve("dayflowj.mv.db");
        Files.writeString(database, "previous-data");
        DesktopFiles.restore(archive, database);
        try (var connection = DriverManager.getConnection("jdbc:h2:file:" + directory.resolve("dayflowj"), "sa", "")) {
            var rows = connection.createStatement().executeQuery("SELECT id FROM tasks");
            assertTrue(rows.next()); assertEquals(7, rows.getInt(1));
        }
        try (var backups = Files.list(directory.resolve("backups"))) {
            assertTrue(backups.anyMatch(file -> file.getFileName().toString().startsWith("pre-restore-")));
        }
    }
}
