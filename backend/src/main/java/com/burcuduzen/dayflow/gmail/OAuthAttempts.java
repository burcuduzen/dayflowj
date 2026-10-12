package com.burcuduzen.dayflow.gmail;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** A short-lived, single-use PKCE attempt works across desktop and system browser sessions. */
@Component
public class OAuthAttempts {
    public record Attempt(String state, String verifier, String challenge) {}
    private record Pending(String verifier, Instant expires) {}
    private final Map<String, Pending> pending = new HashMap<>();
    private final Clock clock;
    public OAuthAttempts() { this(Clock.systemUTC()); }
    OAuthAttempts(Clock clock) { this.clock = clock; }

    public synchronized Attempt create() {
        pending.entrySet().removeIf(entry -> !entry.getValue().expires().isAfter(clock.instant()));
        if (pending.size() >= 16) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Biraz sonra tekrar bağlan.");
        String state = random(), verifier = random();
        String challenge;
        try {
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        pending.put(state, new Pending(verifier, clock.instant().plusSeconds(600)));
        return new Attempt(state, verifier, challenge);
    }

    public synchronized String consume(String state) {
        Pending attempt = state == null ? null : pending.remove(state);
        if (attempt == null || !attempt.expires().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gmail bağlantı isteği geçersiz veya süresi dolmuş. Tekrar bağlan.");
        }
        return attempt.verifier();
    }

    private String random() {
        byte[] value = new byte[32];
        new SecureRandom().nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
