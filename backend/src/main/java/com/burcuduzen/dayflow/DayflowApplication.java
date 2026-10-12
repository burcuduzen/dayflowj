package com.burcuduzen.dayflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class DayflowApplication {

	public static void main(String[] args) throws Exception {
		if (args.length == 2 && args[0].startsWith("--restore-backup=") && args[1].startsWith("--restore-target=")) {
			com.burcuduzen.dayflow.desktop.DesktopFiles.restore(
				java.nio.file.Path.of(args[0].substring("--restore-backup=".length())),
				java.nio.file.Path.of(args[1].substring("--restore-target=".length())));
			return;
		}
		String dataDirectory = System.getenv("DAYFLOW_DATA_DIR");
		if (dataDirectory != null && !dataDirectory.isBlank()) {
			com.burcuduzen.dayflow.desktop.DesktopFiles.prepare(dataDirectory, System.getenv("DAYFLOW_LEGACY_DATA_DIR"));
			System.setProperty("spring.datasource.url", "jdbc:h2:file:" +
				java.nio.file.Path.of(dataDirectory, "dayflowj").toAbsolutePath().toString().replace('\\', '/'));
		}
		SpringApplication.run(DayflowApplication.class, args);
	}

}
