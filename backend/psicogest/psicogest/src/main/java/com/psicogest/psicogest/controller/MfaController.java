package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.auth.*;
import com.psicogest.psicogest.model.enums.LoginStatus;
import com.psicogest.psicogest.security.refresh.RefreshCookieService;
import com.psicogest.psicogest.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/auth/mfa")
public class MfaController {
    private final MfaService mfa;
    private final RefreshCookieService cookies;
    public MfaController(MfaService mfa, RefreshCookieService cookies) { this.mfa = mfa; this.cookies = cookies; }

    @PostMapping("/enrollment")
    public LoginResponse enrollment(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordRequest dto,
            HttpServletRequest request) {
        return new LoginResponse(LoginStatus.MFA_ENROLLMENT_REQUIRED, null,
                mfa.enrollment(Long.valueOf(jwt.getSubject()), dto.password(), request.getRemoteAddr(),
                        request.getHeader("User-Agent")));
    }

    @PostMapping("/totp/setup")
    public MfaService.SetupResult setup(@Valid @RequestBody ChallengeRequest dto) {
        return mfa.setup(dto.challenge());
    }

    @PostMapping("/totp/confirm")
    public EnrollmentResponse confirm(@Valid @RequestBody CodeRequest dto,
            HttpServletRequest request, HttpServletResponse response) {
        var result = mfa.confirm(dto.challenge(), dto.code(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        cookies.write(response, result.tokens().refreshToken());
        return new EnrollmentResponse(result.tokens().response(), result.recoveryCodes());
    }

    @PostMapping("/totp/verify")
    public AuthResponse verify(@Valid @RequestBody CodeRequest dto,
            HttpServletRequest request, HttpServletResponse response) {
        var result = mfa.verify(dto.challenge(), dto.code(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        cookies.write(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/recovery")
    public AuthResponse recover(@Valid @RequestBody CodeRequest dto,
            HttpServletRequest request, HttpServletResponse response) {
        var result = mfa.recover(dto.challenge(), dto.code(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        cookies.write(response, result.refreshToken());
        return result.response();
    }

    @DeleteMapping("/totp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CodeRequest dto,
            HttpServletResponse response) {
        mfa.remove(Long.valueOf(jwt.getSubject()), dto.challenge(), dto.code());
        cookies.clear(response);
    }

    public record ChallengeRequest(@NotBlank @Size(max = 100) String challenge) {}
    // Malformed codes are passed to the service so they also consume a challenge attempt.
    public record CodeRequest(@NotBlank @Size(max = 100) String challenge, @Size(max = 100) String code) {}
    public record PasswordRequest(@NotBlank @Size(max = 200) String password) {}
    public record EnrollmentResponse(AuthResponse authentication, List<String> recoveryCodes) {}
}
