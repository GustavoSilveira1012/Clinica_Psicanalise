package com.psicogest.psicogest.security.refresh;

import com.psicogest.psicogest.security.jwt.JwtProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class RefreshCookieService {

    private final JwtProperties properties;

    public RefreshCookieService(
            JwtProperties properties
    ) {

        this.properties =
                properties;
    }

    public void write(
            HttpServletResponse response,
            String token
    ) {

        ResponseCookie cookie =
                ResponseCookie
                        .from(
                                properties
                                        .refreshCookieName(),
                                token
                        )

                        .httpOnly(true)

                        .secure(
                                properties
                                        .secureCookie()
                        )

                        .sameSite("Strict")

                        .path("/auth")

                        .maxAge(
                                properties
                                        .refreshTokenTtl()
                        )

                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public void clear(
            HttpServletResponse response
    ) {

        ResponseCookie cookie =
                ResponseCookie
                        .from(
                                properties
                                        .refreshCookieName(),
                                ""
                        )

                        .httpOnly(true)

                        .secure(
                                properties
                                        .secureCookie()
                        )

                        .sameSite("Strict")

                        .path("/auth")

                        .maxAge(0)

                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}