package com.psicogest.psicogest.controller;
import com.psicogest.psicogest.security.refresh.RefreshCookieService;
import com.psicogest.psicogest.service.UserSessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/auth/sessions")
public class UserSessionController {
    private final UserSessionService sessions;
    private final RefreshCookieService cookies;
    public UserSessionController(UserSessionService sessions, RefreshCookieService cookies) {
        this.sessions = sessions; this.cookies = cookies;
    }
    @GetMapping
    public List<UserSessionService.SessionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return sessions.list(Long.valueOf(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("sid")));
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, HttpServletResponse response) {
        sessions.revoke(Long.valueOf(jwt.getSubject()), id, "USER_REVOKED");
        if (id.toString().equals(jwt.getClaimAsString("sid"))) cookies.clear(response);
    }
}
