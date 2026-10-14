package com.burcuduzen.dayflow.diagnostics;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** The developer page is unavailable in the normal application. */
@Component
public class DiagnosticsPageGuard extends OncePerRequestFilter {
    @Value("${dayflow.diagnostics.enabled:false}")private boolean enabled;
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().equals("/diagnostics.html");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,jakarta.servlet.FilterChain chain)throws jakarta.servlet.ServletException,IOException {
        if(!enabled){response.sendError(404);return;}
        response.setHeader("Cache-Control","no-store");chain.doFilter(request,response);
    }
}
