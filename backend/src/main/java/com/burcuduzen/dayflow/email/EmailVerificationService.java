package com.burcuduzen.dayflow.email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.security.*;
import java.util.*;

@Service
public class EmailVerificationService {
    private final EmailPreferencesRepository repository;
    private final MailTransport mail;
    private final String baseUrl;
    public EmailVerificationService(EmailPreferencesRepository repository,MailTransport mail,@Value("${dayflow.public-url:http://127.0.0.1:8080}") String baseUrl) {
        this.repository=repository;this.mail=mail;this.baseUrl=baseUrl.replaceAll("/+$","");
    }
    @Transactional public synchronized void connect(String recipient) {
        if(!mail.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Maille hatırlatma henüz hazır değil. Görevlerini kullanmaya devam edebilirsin.");
        var settings=repository.findById(1L).orElseGet(EmailPreferences::new);
        if(settings.lastVerificationSent!=null && settings.lastVerificationSent.isAfter(OffsetDateTime.now().minusMinutes(1)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Yeni mail için bir dakika bekle.");
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        settings.pendingRecipient=recipient.strip().toLowerCase(Locale.ROOT);settings.verificationHash=digest(token);
        settings.verificationExpires=OffsetDateTime.now().plusHours(1);settings.lastVerificationSent=OffsetDateTime.now();
        try { mail.send(settings.pendingRecipient,"DayFlowJ · Maille hatırlatmayı aç","Görev hatırlatmalarını açmak için bağlantıyı bu bilgisayarda aç:\n\n"+baseUrl+"/api/email/verify?token="+token+"\n\nBağlantı 1 saat geçerlidir. Bu isteği sen yapmadıysan maili yok say."); }
        catch(RuntimeException ex) {
            String reason="Mail sunucusuna gönderim başarısız. SMTP sunucusu, port ve bağlantı ayarlarını kontrol et.";
            if(ex instanceof org.springframework.mail.MailAuthenticationException)
                reason="Gmail gönderici girişini kabul etmedi. Gmail adresini ve 16 karakterlik Google uygulama şifresini kontrol et; normal hesap şifreni kullanma.";
            else {
                Throwable cause=ex;
                for(int i=0;cause!=null && i<12;i++,cause=cause.getCause()) {
                    if(cause instanceof javax.net.ssl.SSLException) {
                        reason="Mail sunucusuyla güvenli bağlantı kurulamadı. Java sertifika deposunu ve ağ bağlantısını kontrol et.";break;
                    }
                    if(cause instanceof java.net.ConnectException || cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.UnknownHostException) {
                        reason="Mail sunucusuna ulaşılamadı. İnternet bağlantısını ve SMTP portuna erişimi kontrol et.";break;
                    }
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,reason);
        }
        repository.save(settings);
    }
    @Transactional public synchronized void verify(String token) {
        var settings=repository.findById(1L).orElse(null);
        if(token.length()>128 || settings==null || settings.pendingRecipient==null || settings.verificationHash==null || settings.verificationExpires==null || settings.verificationExpires.isBefore(OffsetDateTime.now())
            || !MessageDigest.isEqual(settings.verificationHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),digest(token).getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bağlantı geçersiz veya süresi dolmuş.");
        settings.recipient=settings.pendingRecipient;settings.verified=true;settings.enabled=true;
        settings.pendingRecipient=null;settings.verificationHash=null;settings.verificationExpires=null;repository.save(settings);
    }
    public static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException ex){throw new IllegalStateException("Doğrulama işlenemedi.");}
    }
}
