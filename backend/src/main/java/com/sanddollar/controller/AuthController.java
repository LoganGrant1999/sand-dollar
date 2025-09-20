package com.sanddollar.controller;

import com.sanddollar.dto.AuthRequest;
import com.sanddollar.dto.AuthResponse;
import com.sanddollar.dto.RegisterRequest;
import com.sanddollar.entity.RefreshToken;
import com.sanddollar.entity.User;
import com.sanddollar.repository.RefreshTokenRepository;
import com.sanddollar.security.JwtUtils;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        if (userService.existsByEmail(request.email())) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Email is already taken!"));
        }

        try {
            User user = userService.createUser(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
            );

            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                ));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            String jwt = jwtUtils.generateJwtToken(user.getEmail(), user.getId());
            String refreshTokenStr = UUID.randomUUID().toString();

            // Clean up old refresh tokens
            refreshTokenRepository.deleteByUser(user);

            // Create new refresh token
            RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenStr,
                Instant.now().plusMillis(jwtUtils.getJwtRefreshExpirationMs())
            );
            refreshTokenRepository.save(refreshToken);

            // Set httpOnly cookies with dynamic secure/sameSite based on request protocol
            setCookie(httpRequest, response, "accessToken", jwt, (int) (jwtUtils.getJwtExpirationMs() / 1000));
            setCookie(httpRequest, response, "refreshToken", refreshTokenStr, (int) (jwtUtils.getJwtRefreshExpirationMs() / 1000));

            return ResponseEntity.ok(new AuthResponse(
                jwt,
                "Bearer",
                jwtUtils.getJwtExpirationMs(),
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Error: Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = getRefreshTokenFromRequest(request);
        
        if (refreshTokenStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Refresh token is missing"));
        }

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshTokenStr);
        
        if (tokenOpt.isPresent() && !tokenOpt.get().isExpired()) {
            RefreshToken token = tokenOpt.get();
            User user = token.getUser();
            String jwt = jwtUtils.generateJwtToken(user.getEmail(), user.getId());

            setCookie(request, response, "accessToken", jwt, (int) (jwtUtils.getJwtExpirationMs() / 1000));

            return ResponseEntity.ok(new AuthResponse(
                jwt,
                "Bearer",
                jwtUtils.getJwtExpirationMs(),
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName())
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Invalid or expired refresh token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Not authenticated"));
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        
        return ResponseEntity.ok(new AuthResponse.UserInfo(
            user.getId(), 
            user.getEmail(), 
            user.getFirstName(), 
            user.getLastName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = getRefreshTokenFromRequest(request);
        
        if (refreshTokenStr != null) {
            refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshTokenRepository::delete);
        }

        clearCookie(request, response, "accessToken");
        clearCookie(request, response, "refreshToken");

        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int maxAge) {
        // Determine if request is secure (HTTPS) by checking isSecure() or X-Forwarded-Proto header
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = secure ? "None" : "Lax";

        var cookie = org.springframework.http.ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        // Determine if request is secure (HTTPS) by checking isSecure() or X-Forwarded-Proto header
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = secure ? "None" : "Lax";

        var cookie = org.springframework.http.ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String getRefreshTokenFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public record MessageResponse(String message) {}
}
