package com.burcuduzen.dayflow.diagnostics;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Performance page is available in the regular application without caching. */
@Component
public class DiagnosticsPageGuard extends OncePerRequestFilter {
    
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().equals("/diagnostics.html");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,jakarta.servlet.FilterChain chain)throws jakarta.servlet.ServletException,IOException {
        response.setHeader("Cache-Control","no-store");chain.doFilter(request,response);
    }
}
