package com.burcuduzen.dayflow.auth;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class AuthWebConfig implements WebMvcConfigurer {
    private final AuthGuard guard;
    public AuthWebConfig(AuthGuard guard) { this.guard=guard; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(guard).addPathPatterns("/api/**"); }
}
