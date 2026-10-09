package com.burcuduzen.dayflow.auth;
import com.burcuduzen.dayflow.email.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.security.SecureRandom;
import java.util.*;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final EmailPreferencesRepository preferences;
    private final MailTransport mail;
    private final String baseUrl;
    private final String dummyHash=PasswordHasher.hash("unmatched-local-account-password");
    public AccountService(AccountRepository accounts,EmailPreferencesRepository preferences,MailTransport mail,
        @Value("${dayflow.public-url:http://127.0.0.1:8080}") String baseUrl) {
        this.accounts=accounts;this.preferences=preferences;this.mail=mail;this.baseUrl=baseUrl.replaceAll("/+$","");
    }
    public boolean exists() { return accounts.existsById(1L); }
    public LocalAccount current() { return accounts.findById(1L).orElse(null); }
    public boolean mailReady() { return mail.configured(); }
    @Transactional public synchronized void register(String email,String password,boolean reminders) {
        if(exists()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Bu yerel uygulamada hesap zaten var. Giriş yap.");
        requireMail();
        var account=new LocalAccount();account.email=email.strip().toLowerCase(Locale.ROOT);account.passwordHash=PasswordHasher.hash(password);
        String token=issueToken(account);
        accounts.saveAndFlush(account);
        var settings=preferences.findById(1L).orElseGet(EmailPreferences::new);
        settings.recipient=account.email;settings.enabled=reminders;preferences.save(settings);
        sendVerification(account.email,token);
    }
    public LocalAccount authenticate(String email,String password) {
        LocalAccount account=current();
        boolean valid=PasswordHasher.matches(password,account==null?dummyHash:account.passwordHash);
        if(account==null || !valid || !account.email.equals(email.strip().toLowerCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Mail veya parola hatalı.");
        return account;
    }
    @Transactional public synchronized void resend(String email,String password) {
        var account=authenticate(email,password);
        if(account.verified) return;
        requireMail();
        if(account.lastVerificationSent!=null && account.lastVerificationSent.isAfter(OffsetDateTime.now().minusMinutes(1)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Tekrar göndermek için bir dakika bekle.");
        String token=issueToken(account);accounts.save(account);sendVerification(account.email,token);
    }
    @Transactional public synchronized void verify(String token) {
        var account=current();
        if(account==null || account.verified || account.verificationHash==null || account.verificationExpires==null
            || account.verificationExpires.isBefore(OffsetDateTime.now())
            || !java.security.MessageDigest.isEqual(account.verificationHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),PasswordHasher.digest(token).getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Doğrulama bağlantısı geçersiz veya süresi dolmuş.");
        account.verified=true;account.verificationHash=null;account.verificationExpires=null;accounts.save(account);
    }
    private String issueToken(LocalAccount account) {
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        account.verificationHash=PasswordHasher.digest(token);account.verificationExpires=OffsetDateTime.now().plusHours(1);account.lastVerificationSent=OffsetDateTime.now();return token;
    }
    private void requireMail() {
        if(!mail.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"DayFlowJ mail göndericisi henüz hazır değil. Kurulum tamamlandığında kayıt olabilirsin.");
    }
    private void sendVerification(String email,String token) {
        try { mail.send(email,"DayFlowJ · Mail adresini doğrula","DayFlowJ hesabını doğrulamak için bağlantıyı aç:\n\n"+baseUrl+"/api/auth/verify?token="+token+"\n\nBağlantı 1 saat geçerlidir. Bu yerel sürümde bağlantıyı uygulamanın çalıştığı bilgisayarda aç. Kaydı sen yapmadıysan bu maili yok say."); }
        catch(RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Doğrulama maili gönderilemedi. Biraz sonra tekrar dene."); }
    }
}
