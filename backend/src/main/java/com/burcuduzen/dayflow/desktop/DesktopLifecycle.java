package com.burcuduzen.dayflow.desktop;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Private parent/child protocol. No HTTP shutdown endpoint is exposed. */
@Component
@ConditionalOnProperty(name = "dayflow.desktop.managed", havingValue = "true")
public class DesktopLifecycle {
    private final ConfigurableApplicationContext context;

    public DesktopLifecycle(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        String token = System.getenv("DAYFLOW_DESKTOP_CONTROL_TOKEN");
        if (token == null || token.length() < 32) throw new IllegalStateException("Desktop control token missing.");
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        System.out.println("DAYFLOW_READY:" + port);
        System.out.flush();
        Thread listener = new Thread(() -> {
            try (var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (MessageDigest.isEqual(line.getBytes(StandardCharsets.UTF_8),
                            ("STOP " + token).getBytes(StandardCharsets.UTF_8))) break;
                }
                // EOF means the owning desktop process has gone away.
                context.close();
            } catch (Exception ignored) {
                context.close();
            }
        }, "desktop-lifecycle");
        listener.setDaemon(true);
        listener.start();
    }
}
