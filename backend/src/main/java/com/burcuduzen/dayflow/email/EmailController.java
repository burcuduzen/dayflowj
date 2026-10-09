package com.burcuduzen.dayflow.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/email")
public class EmailController {
    public record PreferencesRequest(boolean enabled) {}
    public record PreferencesResponse(boolean enabled, String recipient, boolean smtpConfigured) {}
    private final EmailPreferencesRepository repository;
    private final MailTransport mail;
    private final com.burcuduzen.dayflow.auth.AccountService accounts;
    public EmailController(EmailPreferencesRepository repository, MailTransport mail, com.burcuduzen.dayflow.auth.AccountService accounts) { this.repository = repository; this.mail = mail; this.accounts = accounts; }
    @GetMapping("/settings") public PreferencesResponse get() {
        var preferences = repository.findById(1L).orElseGet(EmailPreferences::new);
        return new PreferencesResponse(preferences.enabled, accounts.current().email, mail.configured());
    }
    @PutMapping("/settings") public PreferencesResponse update(@Valid @RequestBody PreferencesRequest request) {
        var preferences = repository.findById(1L).orElseGet(EmailPreferences::new);
        preferences.recipient = accounts.current().email; preferences.enabled = request.enabled(); repository.save(preferences);
        return get();
    }
    @PostMapping("/test") public ResponseEntity<Void> test() {
        var preferences = repository.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Önce e-posta adresini kaydet."));
        if (!mail.configured()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DayFlowJ mail göndericisi henüz hazır değil.");
        try { mail.send(accounts.current().email, "DayFlowJ · Bağlantı testi", "DayFlowJ e-posta bağlantın çalışıyor. Yaklaşan görevlerin için hatırlatma alabilirsin."); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mail gönderilemedi. Biraz sonra tekrar dene."); }
        return ResponseEntity.noContent().build();
    }
}
