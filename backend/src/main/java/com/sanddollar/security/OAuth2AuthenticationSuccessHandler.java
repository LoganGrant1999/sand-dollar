package com.sanddollar.security;

import com.sanddollar.entity.User;
import com.sanddollar.security.JwtUtils;
import com.sanddollar.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

        System.out.println("=================== OAuth2AuthenticationSuccessHandler CALLED ===================");

        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            // Extract user information from OIDC token
            String sub = oidcUser.getSubject();
            String email = oidcUser.getEmail();
            String firstName = oidcUser.getGivenName();
            String lastName = oidcUser.getFamilyName();

            // Determine provider based on issuer
            String issuer = oidcUser.getIssuer().toString();
            String provider = determineProvider(issuer);

            // Find or create user
            User user = userService.findOrCreateOAuthUser(provider, sub, email, firstName, lastName);

            // Generate our own JWT token
            String jwt = jwtUtils.generateJwtToken(user.getEmail(), user.getId());
            System.out.println("=================== Generated JWT token: " + jwt.substring(0, 20) + "...");

            System.out.println("=================== Request URL: " + request.getRequestURL());
            System.out.println("=================== X-Forwarded-Proto: " + request.getHeader("X-Forwarded-Proto"));
            System.out.println("=================== X-Forwarded-Host: " + request.getHeader("X-Forwarded-Host"));

            // Determine the correct redirect URL based on environment
            String baseUrl = determineBaseUrl(request);
            String redirect = baseUrl + "/oauth-success#t=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            System.out.println("=================== Redirecting with token fragment to: " + redirect.substring(0, redirect.indexOf("#t=") + 3) + "...");
            response.sendRedirect(redirect);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private String determineProvider(String issuer) {
        if (issuer.contains("accounts.google.com")) {
            return "GOOGLE";
        } else if (issuer.contains("appleid.apple.com")) {
            return "APPLE";
        }
        return "UNKNOWN";
    }

    private String determineBaseUrl(HttpServletRequest request) {
        // Check if this is coming through ngrok (has the X-Forwarded-Host header)
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");

        if (forwardedHost != null && forwardedProto != null) {
            // This is coming through ngrok, use the forwarded URL
            return forwardedProto + "://" + forwardedHost;
        }

        // For local development, use configured frontend URL
        return frontendUrl;
    }


}