package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.auth.AuthResponse;
import com.psicogest.psicogest.dto.auth.CsrfResponse;
import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.security.refresh.RefreshCookieService;
import com.psicogest.psicogest.service.AuthService;
import com.psicogest.psicogest.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService cookieService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthService authService,
            RefreshCookieService cookieService,
            RefreshTokenService refreshTokenService
    ) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthTokens tokens = authService.login(
                dto,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        cookieService.write(response, tokens.refreshToken());
        return tokens.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = "${app.security.jwt.refresh-cookie-name}")
            String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthTokens tokens = authService.refresh(
                refreshToken,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        cookieService.write(response, tokens.refreshToken());
        return tokens.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(
                    name = "${app.security.jwt.refresh-cookie-name}",
                    required = false
            ) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            refreshTokenService.revokeCurrentSession(refreshToken);
        }

        cookieService.clear(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response
    ) {
        authService.logoutAll(Long.valueOf(jwt.getSubject()));
        cookieService.clear(response);
    }
}
