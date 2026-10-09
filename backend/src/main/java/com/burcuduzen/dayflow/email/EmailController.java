package com.burcuduzen.dayflow.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/email")
public class EmailController {
    public record PreferencesRequest(boolean enabled, @NotBlank @Email @Size(max = 254) String recipient) {}
    public record PreferencesResponse(boolean enabled, String recipient, boolean smtpConfigured) {}
    private final EmailPreferencesRepository repository;
    private final MailTransport mail;
    public EmailController(EmailPreferencesRepository repository, MailTransport mail) { this.repository = repository; this.mail = mail; }
    @GetMapping("/settings") public PreferencesResponse get() {
        var preferences = repository.findById(1L).orElseGet(EmailPreferences::new);
        return new PreferencesResponse(preferences.enabled, preferences.recipient, mail.configured());
    }
    @PutMapping("/settings") public PreferencesResponse update(@Valid @RequestBody PreferencesRequest request) {
        var preferences = repository.findById(1L).orElseGet(EmailPreferences::new);
        preferences.recipient = request.recipient().strip(); preferences.enabled = request.enabled(); repository.save(preferences);
        return get();
    }
    @PostMapping("/test") public ResponseEntity<Void> test() {
        var preferences = repository.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Önce e-posta adresini kaydet."));
        if (!mail.configured()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP ayarları eksik. Kurulum belgesine bak.");
        try { mail.send(preferences.recipient, "DayFlowJ · Bağlantı testi", "DayFlowJ e-posta bağlantın çalışıyor. Yaklaşan görevlerin için hatırlatma alabilirsin."); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "E-posta gönderilemedi. SMTP ayarlarını ve uygulama parolanı kontrol et."); }
        return ResponseEntity.noContent().build();
    }
}
