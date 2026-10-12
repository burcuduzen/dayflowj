package com.burcuduzen.dayflow.desktop;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/backups")
public class BackupController {
    private final JdbcTemplate jdbc;
    public BackupController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping("/export")
    public synchronized ResponseEntity<FileSystemResource> export() throws IOException {
        String configured = System.getenv("DAYFLOW_DATA_DIR");
        Path backups = Path.of(configured == null ? "data" : configured, "backups").toAbsolutePath().normalize();
        Files.createDirectories(backups);
        Path target = backups.resolve("dayflowj-" + System.currentTimeMillis() + ".zip");
        jdbc.execute("BACKUP TO '" + target.toString().replace("'", "''") + "'");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dayflowj-backup.zip\"")
            .body(new FileSystemResource(target));
    }
}
