package com.burcuduzen.dayflow.auth;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=12,max=128) String password) {}
    public record Registration(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=12,max=128) String password,boolean reminders) {}
    public record Status(boolean accountExists,boolean authenticated,String email,boolean mailReady) {}
    private final AccountService service;
    public AuthController(AccountService service) { this.service=service; }
    @GetMapping("/status") public Status status(HttpServletRequest request) {
        var account=service.current();var session=request.getSession(false);
        boolean loggedIn=account!=null && account.verified && session!=null && Long.valueOf(1).equals(session.getAttribute("accountId"));
        return new Status(account!=null,loggedIn,loggedIn?account.email:null,service.mailReady());
    }
    @PostMapping("/register") public ResponseEntity<Void> register(@Valid @RequestBody Registration body) {
        service.register(body.email(),body.password(),body.reminders());return ResponseEntity.noContent().build();
    }
    @PostMapping("/login") public Status login(@Valid @RequestBody Credentials body,HttpServletRequest request) {
        var account=service.authenticate(body.email(),body.password());
        if(!account.verified) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Önce mailindeki bağlantıyla adresini doğrula. Gerekirse doğrulama mailini yeniden gönder.");
        var old=request.getSession(false);if(old!=null)old.invalidate();
        var session=request.getSession(true);session.setAttribute("accountId",1L);session.setMaxInactiveInterval(8*60*60);
        return status(request);
    }
    @PostMapping("/resend") public ResponseEntity<Void> resend(@Valid @RequestBody Credentials body) {
        service.resend(body.email(),body.password());return ResponseEntity.noContent().build();
    }
    @GetMapping("/verify") public ResponseEntity<Void> verify(@RequestParam @Size(max=128) String token) {
        try { if (token.length()>128) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST); service.verify(token);return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/?verification=success")).build(); }
        catch(org.springframework.web.server.ResponseStatusException ex) { return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/?verification=invalid")).build(); }
    }
    @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session=request.getSession(false);if(session!=null)session.invalidate();return ResponseEntity.noContent().build();
    }
}
