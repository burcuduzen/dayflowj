package com.burcuduzen.dayflow.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;
import com.burcuduzen.dayflow.calendar.CalendarEventService;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class GmailService {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<com.burcuduzen.dayflow.diagnostics.DiagnosticsService> diagnostics;
    private void audit(String level,String action){if(diagnostics!=null)diagnostics.ifAvailable(d->d.event(level,action));}
    private final String clientId,secret,redirect;
    private final MailCandidateRepository repository;
    private final CalendarEventService calendar;
    private final org.springframework.transaction.support.TransactionTemplate transaction;
    private final RestClient http;
    private final MailEventParser parser=new MailEventParser();
    private String accessToken,refreshToken,account="",lastError="",query="in:inbox newer_than:7d";
    private String nextPage="";
    private boolean automatic=false;
    private Instant expiry=Instant.EPOCH;
    public GmailService(@Value("${dayflow.gmail.client-id:}") String clientId,
                        @Value("${dayflow.gmail.client-secret:}") String secret,
                        @Value("${dayflow.gmail.redirect-uri:http://127.0.0.1:8080/api/gmail/callback}") String redirect,
                        MailCandidateRepository repository, CalendarEventService calendar, org.springframework.transaction.PlatformTransactionManager manager) {
        this.transaction=new org.springframework.transaction.support.TransactionTemplate(manager);this.clientId=clientId;this.secret=secret;this.redirect=redirect;this.repository=repository;this.calendar=calendar;
        var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(
            java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));http=RestClient.builder().requestFactory(factory).build();
    }
    public synchronized Map<String,Object> status() {
        return Map.of("configured",!clientId.isBlank()&&!secret.isBlank(),"connected",accessToken!=null,
            "account",account,"automatic",automatic,"query",query,"error",lastError,
            "reconnectAfterRestart",true);
    }
    public String authorize(String state,String challenge) {
        if(clientId.isBlank()||secret.isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Google bağlantısı henüz yapılandırılmadı. Geliştirici Gmail API ve OAuth bilgilerini ayarlamalı.");
        return "https://accounts.google.com/o/oauth2/v2/auth?client_id="+enc(clientId)+"&redirect_uri="+enc(redirect)
            +"&response_type=code&scope="+enc("https://www.googleapis.com/auth/gmail.readonly")
            +"&access_type=offline&prompt=consent&state="+enc(state)+"&code_challenge="+enc(challenge)+"&code_challenge_method=S256";
    }
    public synchronized void callback(String code,String verifier) {
        var form=new LinkedMultiValueMap<String,String>();form.add("code",code);form.add("code_verifier",verifier);
        form.add("redirect_uri",redirect);form.add("grant_type","authorization_code");
        Map<?,?> tokens=token(form);String nextAccess=(String)tokens.get("access_token");
        if(nextAccess==null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Google bağlantısı tamamlanamadı.");
        Map<?,?> profile=http.get().uri("https://gmail.googleapis.com/gmail/v1/users/me/profile")
            .header("Authorization","Bearer "+nextAccess).retrieve().body(Map.class);
        accessToken=nextAccess;refreshToken=(String)tokens.get("refresh_token");
        expiry=Instant.now().plusSeconds(((Number)tokens.getOrDefault("expires_in",null)).longValue()-60);
        account=Objects.toString(profile.get("emailAddress"),"");lastError="";automatic=false;nextPage="";
    }
    private Map<?,?> token(LinkedMultiValueMap<String,String> form) {
        form.add("client_id",clientId);form.add("client_secret",secret);
        return http.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form).retrieve().body(Map.class);
    }
    private void refresh() {
        if(accessToken==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Önce Gmail hesabını bağla.");
        if(Instant.now().isBefore(expiry))return;
        if(refreshToken==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Gmail bağlantısını yenile.");
        var form=new LinkedMultiValueMap<String,String>();form.add("refresh_token",refreshToken);form.add("grant_type","refresh_token");
        var tokens=token(form);accessToken=(String)tokens.get("access_token");
        expiry=Instant.now().plusSeconds(((Number)tokens.get("expires_in")).longValue()-60);
    }
    public synchronized void configure(String query,boolean automatic) {
        if(query==null||query.isBlank()||query.length()>500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Geçerli bir Gmail filtresi gir.");
        if(!this.query.equals(query.strip()))nextPage="";this.query=query.strip();this.automatic=automatic;
    }
    public synchronized void disconnect() { accessToken=null;refreshToken=null;account="";automatic=false;lastError=""; }
    private Map<?,?> get(String path) { return http.get().uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/"+path))
        .header("Authorization","Bearer "+accessToken).retrieve().body(Map.class); }
    public synchronized Map<String,Object> sync() {
        refresh();int added=0,pending=0;String page=nextPage;int pages=0;
        try {
            do {
                Map<?,?> list=get("messages?maxResults=100&q="+enc(query)+(page.isEmpty()?"":"&pageToken="+enc(page)));
                if(list.get("messages") instanceof List<?> messages) for(Object item:messages) {
                    String id=Objects.toString(((Map<?,?>)item).get("id"));
                    if(!id.matches("[a-zA-Z0-9_-]{1,100}")||repository.existsByAccountAndMessageId(account,id))continue;
                    Map<?,?> mail=get("messages/"+id+"?format=full");
                    if(!(mail.get("payload") instanceof Map<?,?> payload))continue;
                    String subject="",body=body(payload);
                    if(payload.get("headers") instanceof List<?> headers)for(Object raw:headers) {
                        Map<?,?> header=(Map<?,?>)raw;if("Subject".equalsIgnoreCase(Objects.toString(header.get("name"))))subject=Objects.toString(header.get("value"),"");
                    }
                    // Exclude quoted replies so old appointments do not create new events.
                    body=body.split("(?m)^>|(?m)^On .+wrote:|(?m)^-{2,}.*",2)[0];
                    var sent=Instant.ofEpochMilli(Long.parseLong(Objects.toString(mail.get("internalDate")))).atZone(ZoneId.of("Europe/Istanbul"));
                    var draft=parser.parse(subject,body,sent,Instant.now());if(draft==null)continue;
                    var candidate=new MailCandidate(account,id,draft);
                    boolean shouldAdd=automatic&&draft.automatic();
                    transaction.executeWithoutResult(tx->{if(shouldAdd)createEvent(candidate);repository.save(candidate);});
                    if(shouldAdd)added++;else pending++;
                }
                page=Objects.toString(list.get("nextPageToken"),"");
            }while(!page.isEmpty()&&++pages<5);
            nextPage=page;lastError="";audit("INFO","Gmail kontrolü tamamlandı · "+added+" etkinlik eklendi · "+pending+" öneri");return Map.of("added",added,"pending",pending,"more",!page.isEmpty());
        }catch(RuntimeException ex){audit("ERROR","Gmail kontrolü başarısız");lastError="Gmail kontrolü tamamlanamadı. Bağlantını ve filtreyi kontrol et.";throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,lastError);}
    }
    private String body(Map<?,?> payload) {
        StringBuilder text=new StringBuilder();
        if("text/plain".equals(payload.get("mimeType"))&&payload.get("body") instanceof Map<?,?> b&&b.get("data") instanceof String data) {
            try {text.append(new String(Base64.getUrlDecoder().decode(data),StandardCharsets.UTF_8));}catch(IllegalArgumentException ignored){}
        }
        if(payload.get("parts") instanceof List<?> parts)for(Object p:parts)if(p instanceof Map<?,?> part)text.append('\n').append(body(part));
        return text.substring(0,Math.min(text.length(),20000));
    }
    private void createEvent(MailCandidate c) {
        c.eventId=calendar.create(c.title,"Gmail kaynağı: https://mail.google.com/mail/u/0/#all/"+c.messageId+"\n"+c.reason,c.startAt,c.endAt).getId();c.status="ADDED";
    }
    public synchronized List<MailCandidate> candidates() {return account.isBlank()?List.of():repository.findTop100ByAccountOrderByIdDesc(account);}
    public synchronized MailCandidate approve(Long id,String title,OffsetDateTime start,OffsetDateTime end) {
        var c=find(id);if(!"PENDING".equals(c.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Bu mail zaten işlendi.");
        return transaction.execute(tx->{c.title=title;c.startAt=start;c.endAt=end;createEvent(c);return repository.save(c);});
    }
    public synchronized void dismiss(Long id) {var c=find(id);if("PENDING".equals(c.status)){c.status="DISMISSED";repository.save(c);}}
    private MailCandidate find(Long id) {return repository.findById(id).filter(c->!account.isBlank()&&account.equals(c.account))
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Mail kaydı bulunamadı."));}
    @Scheduled(fixedDelay=300000,initialDelay=60000) public void poll() {
        synchronized(this){if(accessToken==null)return;try{sync();}catch(RuntimeException ignored){audit("WARN","Zamanlanmış Gmail kontrolü tamamlanamadı");}}
    }
    static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
