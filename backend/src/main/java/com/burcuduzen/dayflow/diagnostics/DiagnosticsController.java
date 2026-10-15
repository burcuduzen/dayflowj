package com.burcuduzen.dayflow.diagnostics;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@RestController @RequestMapping("/api/diagnostics")
public class DiagnosticsController {
    private final DiagnosticsService service;
    public DiagnosticsController(DiagnosticsService service){this.service=service;}
    @GetMapping("/metrics")public Map<String,Object> metrics(){return service.snapshot();}
    @GetMapping("/logs")public List<DiagnosticStore.Entry> logs(@RequestParam(required=false)String level,@RequestParam(required=false)String search,@RequestParam(defaultValue="100")int limit){return service.entries(level,search,limit);}
}
@Component
class DiagnosticsFilter extends OncePerRequestFilter {
    private final DiagnosticsService service;
    DiagnosticsFilter(DiagnosticsService service){this.service=service;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,jakarta.servlet.FilterChain chain)throws jakarta.servlet.ServletException,IOException {
        long start=System.nanoTime();boolean failed=false;
        try{chain.doFilter(request,response);}catch(jakarta.servlet.ServletException|IOException|RuntimeException ex){failed=true;throw ex;}
        finally{service.request(request.getMethod(),request.getRequestURI(),failed?500:response.getStatus(),(System.nanoTime()-start)/1_000_000.0);}
    }
}
