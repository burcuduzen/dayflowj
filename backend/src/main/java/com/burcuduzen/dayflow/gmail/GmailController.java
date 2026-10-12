package com.burcuduzen.dayflow.gmail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.security.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/gmail")
public class GmailController {
    private final GmailService service;
    private final OAuthAttempts attempts;
    @org.springframework.beans.factory.annotation.Value("${dayflow.desktop.managed:false}") private boolean desktopManaged;
    public GmailController(GmailService service, OAuthAttempts attempts){this.service=service;this.attempts=attempts;}
    @GetMapping("/status") public Map<String,Object> status(){return service.status();}
    @PostMapping("/connect") public Map<String,String> connect(jakarta.servlet.http.HttpServletRequest request) {
        if(!"127.0.0.1".equals(request.getServerName()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Gmail bağlantısı için uygulamayı http://127.0.0.1:8080 adresinden aç.");
        var attempt=attempts.create();
        String url=service.authorize(attempt.state(),attempt.challenge());
        return Map.of("url",url);
    }
    @GetMapping("/callback") public ResponseEntity<?> callback(@RequestParam(required=false)String code,
        @RequestParam(required=false)String state,@RequestParam(required=false)String error) {
        String verifier=attempts.consume(state);
        String result="cancelled";
        if(error==null&&code!=null) {try{service.callback(code,verifier);result="connected";}catch(RuntimeException ex){result="failed";}}
        if(desktopManaged) return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(
            result.equals("connected") ? "Google bağlantısı tamamlandı. DayFlowJ penceresine dönebilirsin." : "Google bağlantısı tamamlanamadı. DayFlowJ penceresine dönüp tekrar dene.");
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Referrer-Policy","no-referrer").location(URI.create("/?gmail="+result)).build();
    }
    public record Preferences(@NotBlank @Size(max=500)String query,boolean automatic){}
    @PutMapping("/preferences") public Map<String,Object> preferences(@Valid @RequestBody Preferences p){service.configure(p.query(),p.automatic());return service.status();}
    @PostMapping("/sync") public Map<String,Object> sync(){return service.sync();}
    @DeleteMapping("/connection") public ResponseEntity<Void> disconnect(){service.disconnect();return ResponseEntity.noContent().build();}
    @GetMapping("/candidates") public List<MailCandidate> candidates(){return service.candidates();}
    public record Approval(@NotBlank @Size(max=200)String title,@NotNull OffsetDateTime startAt,@NotNull OffsetDateTime endAt){}
    @PostMapping("/candidates/{id}/approve")public MailCandidate approve(@PathVariable Long id,@Valid @RequestBody Approval p){return service.approve(id,p.title(),p.startAt(),p.endAt());}
    @PostMapping("/candidates/{id}/dismiss")public ResponseEntity<Void> dismiss(@PathVariable Long id){service.dismiss(id);return ResponseEntity.noContent().build();}
}
