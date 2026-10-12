package com.burcuduzen.dayflow.gmail;
import jakarta.servlet.http.HttpSession;
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
    public GmailController(GmailService service){this.service=service;}
    @GetMapping("/status") public Map<String,Object> status(){return service.status();}
    @PostMapping("/connect") public Map<String,String> connect(jakarta.servlet.http.HttpServletRequest request,HttpSession session)throws NoSuchAlgorithmException {
        if(!"127.0.0.1".equals(request.getServerName()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Gmail bağlantısı için uygulamayı http://127.0.0.1:8080 adresinden aç.");
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);
        String state=Base64.getUrlEncoder().withoutPadding().encodeToString(random);new SecureRandom().nextBytes(random);
        String verifier=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        String url=service.authorize(state,challenge);
        session.setAttribute("gmailState",state);session.setAttribute("gmailVerifier",verifier);session.setAttribute("gmailStarted",Instant.now());
        return Map.of("url",url);
    }
    @GetMapping("/callback") public ResponseEntity<Void> callback(@RequestParam(required=false)String code,
        @RequestParam(required=false)String state,@RequestParam(required=false)String error,HttpSession session) {
        String expected=(String)session.getAttribute("gmailState"), verifier=(String)session.getAttribute("gmailVerifier");
        Instant started=(Instant)session.getAttribute("gmailStarted");session.removeAttribute("gmailState");session.removeAttribute("gmailVerifier");session.removeAttribute("gmailStarted");
        if(expected==null||!expected.equals(state)||started==null||started.isBefore(Instant.now().minusSeconds(600)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Gmail bağlantı isteğinin süresi doldu. Tekrar bağlan.");
        String result="cancelled";
        if(error==null&&code!=null) {try{service.callback(code,verifier);result="connected";}catch(RuntimeException ex){result="failed";}}
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
