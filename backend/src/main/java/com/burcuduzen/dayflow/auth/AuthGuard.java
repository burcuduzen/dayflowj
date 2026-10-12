package com.burcuduzen.dayflow.auth;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.*;

/** Local app requests need no login; writes must originate in the app. */
@Component
public class AuthGuard implements HandlerInterceptor {
    private final Map<String,Attempt> attempts=new LinkedHashMap<>();
    private record Attempt(long start,int count) {}
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
        boolean mutation=!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod());
        if(mutation) {
            if(!"1".equals(request.getHeader("X-DayFlowJ-Request"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"İstek doğrulanamadı.");
            String origin=request.getHeader("Origin");
            if(origin!=null) {
                try {
                    URI uri=URI.create(origin);int port=uri.getPort()==-1?("https".equals(uri.getScheme())?443:80):uri.getPort();
                    if(!Objects.equals(uri.getHost(),request.getServerName()) || port!=request.getServerPort() || !Objects.equals(uri.getScheme(),request.getScheme())) throw new IllegalArgumentException();
                } catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN,"İstek kaynağı doğrulanamadı."); }
            }
            if(request.getRequestURI().equals("/api/email/connect") || request.getRequestURI().equals("/api/email/test")) throttle(request.getRemoteAddr());
        }
        return true;
    }
    private synchronized void throttle(String address) {
        long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->now-e.getValue().start()>60000);
        Attempt attempt=attempts.get(address);int count=attempt==null?1:attempt.count()+1;
        if(count>5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bir dakika sonra tekrar dene.");
        if(attempts.size()>1000)attempts.clear();
        attempts.put(address,new Attempt(attempt==null?now:attempt.start(),count));
    }
}
