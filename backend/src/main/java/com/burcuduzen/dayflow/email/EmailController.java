package com.burcuduzen.dayflow.email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;

@RestController @RequestMapping("/api/email")
public class EmailController {
    public record PreferencesRequest(boolean enabled) {}
    public record ConnectRequest(@NotBlank @Email @Size(max=254) String recipient) {}
    public record PreferencesResponse(boolean enabled,String recipient,boolean verified,String pendingRecipient,boolean smtpConfigured) {}
    private final EmailPreferencesRepository repository;
    private final MailTransport mail;
    private final EmailVerificationService verification;
    public EmailController(EmailPreferencesRepository repository,MailTransport mail,EmailVerificationService verification) { this.repository=repository;this.mail=mail;this.verification=verification; }
    @GetMapping("/settings") public PreferencesResponse get() {
        var settings=repository.findById(1L).orElseGet(EmailPreferences::new);
        return new PreferencesResponse(settings.enabled,settings.recipient,settings.verified,settings.pendingRecipient,mail.configured());
    }
    @PostMapping("/connect") public ResponseEntity<Void> connect(@Valid @RequestBody ConnectRequest body) {
        verification.connect(body.recipient());return ResponseEntity.noContent().build();
    }
    @GetMapping("/verify") public ResponseEntity<Void> verify(@RequestParam String token) {
        try { verification.verify(token);return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/?emailVerification=success")).build(); }
        catch(ResponseStatusException ex){return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/?emailVerification=invalid")).build();}
    }
    @PutMapping("/settings") public PreferencesResponse update(@Valid @RequestBody PreferencesRequest request) {
        var settings=repository.findById(1L).orElseGet(EmailPreferences::new);
        if(request.enabled() && !settings.verified)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Önce mail adresini doğrula.");
        settings.enabled=request.enabled();repository.save(settings);return get();
    }
    @PostMapping("/test") public ResponseEntity<Void> test() {
        var settings=repository.findById(1L).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Önce mail adresini doğrula."));
        if(!settings.verified || settings.recipient==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Önce mail adresini doğrula.");
        if(!mail.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Mail gönderimi şu anda hazır değil.");
        try{mail.send(settings.recipient,"DayFlowJ · Test bildirimi","DayFlowJ mail bağlantın çalışıyor. Yaklaşan görevlerini buradan hatırlatacağız.");}
        catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Mail gönderilemedi. Daha sonra tekrar dene.");}
        return ResponseEntity.noContent().build();
    }
}
