package com.sanddollar.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ContentSecurityPolicyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // CSP policy to allow Plaid Link to function properly
        String policy = String.join("; ", new String[]{
            "default-src 'self'",
            "script-src 'self' https://cdn.plaid.com https://*.plaid.com",
            "frame-src 'self' https://cdn.plaid.com https://*.plaid.com",
            "worker-src 'self' blob:",
            "connect-src 'self' https://*.plaid.com",
            "img-src 'self' https://*.plaid.com data:",
            "style-src 'self' 'unsafe-inline'"
        });

        httpResponse.setHeader("Content-Security-Policy", policy);

        chain.doFilter(request, response);
    }
}