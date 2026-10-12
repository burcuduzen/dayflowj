package com.burcuduzen.dayflow.desktop;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.zip.ZipInputStream;
import static java.nio.file.StandardOpenOption.*;

/** Runs before Hibernate opens the database. Existing live databases are never copied. */
public final class DesktopFiles {
    private DesktopFiles() {}

    public static void prepare(String directory, String legacyDirectory) throws IOException {
        Path data = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(data.resolve("backups"));
        Path database = data.resolve("dayflowj.mv.db");
        if (!Files.exists(database) && legacyDirectory != null && !legacyDirectory.isBlank()) {
            Path legacy = Path.of(legacyDirectory).toAbsolutePath().normalize();
            Path source = legacy.resolve("dayflowj.mv.db");
            if (Files.exists(source) && !source.equals(database)) {
                copyClosedDatabase(source, database);
                Path credentials = legacy.resolve("desktop-settings.dpapi");
                if (Files.exists(credentials) && !Files.exists(data.resolve(credentials.getFileName()))) {
                    Files.copy(credentials, data.resolve(credentials.getFileName()));
                }
            }
        }
        if (Files.exists(database)) {
            String stamp = Instant.now().toString().replace(':', '-');
            copyClosedDatabase(database, data.resolve("backups/pre-start-" + stamp + ".mv.db"));
            try (var backups = Files.list(data.resolve("backups"))) {
                var obsolete = backups.filter(file -> file.getFileName().toString().startsWith("pre-start-") && file.toString().endsWith(".mv.db"))
                    .sorted(java.util.Comparator.reverseOrder()).skip(7).toList();
                for (Path file : obsolete) Files.deleteIfExists(file);
            }
        }
    }

    private static void copyClosedDatabase(Path source, Path destination) throws IOException {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".pending");
        try (FileChannel channel = FileChannel.open(source, READ, WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IOException("Eski DayFlowJ hâlâ açık. Önce eski uygulamayı ve backend terminalini kapat.");
            try (var output = FileChannel.open(temporary, CREATE_NEW, WRITE)) {
                long position = 0;
                while (position < channel.size()) {
                    long count = channel.transferTo(position, channel.size() - position, output);
                    if (count <= 0) throw new IOException("Database copy did not complete.");
                    position += count;
                }
                output.force(true);
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | java.nio.channels.OverlappingFileLockException exception) {
            Files.deleteIfExists(temporary);
            throw new IOException("Veritabanı güvenle kopyalanamadı. Eski DayFlowJ/backend açıksa kapatıp tekrar dene.", exception);
        }
    }

    public static void restore(Path archive, Path database) throws Exception {
        Path data = database.toAbsolutePath().normalize().getParent();
        Files.createDirectories(data.resolve("backups"));
        Path candidate = Files.createTempFile(data, "restore-", ".mv.db");
        boolean found = false;
        try {
            try (var zip = new ZipInputStream(Files.newInputStream(archive))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    if (found || !entry.getName().equals("dayflowj.mv.db")) {
                        throw new IOException("Yedek yalnızca DayFlowJ veritabanını içermeli.");
                    }
                    found = true;
                    try (var output = Files.newOutputStream(candidate, TRUNCATE_EXISTING, WRITE)) {
                        byte[] buffer = new byte[65536];
                        long total = 0;
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            total += count;
                            if (total > 512L * 1024 * 1024) throw new IOException("Yedek boyutu sınırı aşıldı.");
                            output.write(buffer, 0, count);
                        }
                    }
                }
            }
            if (!found) throw new IOException("Yedekte DayFlowJ veritabanı bulunamadı.");
            String base = candidate.toString().substring(0, candidate.toString().length() - ".mv.db".length()).replace('\\', '/');
            try (var connection = DriverManager.getConnection("jdbc:h2:file:" + base + ";IFEXISTS=TRUE", "sa", "");
                 var statement = connection.createStatement()) {
                statement.executeQuery("SELECT COUNT(*) FROM tasks").close();
            }
            if (Files.exists(database)) {
                copyClosedDatabase(database, data.resolve("backups/pre-restore-" + System.currentTimeMillis() + ".mv.db"));
            }
            Files.move(candidate, database, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(candidate);
        }
    }
}
