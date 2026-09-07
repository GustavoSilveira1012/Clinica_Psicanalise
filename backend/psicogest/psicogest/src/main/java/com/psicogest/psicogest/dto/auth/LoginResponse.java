package com.psicogest.psicogest.dto.auth;

import com.psicogest.psicogest.model.enums.LoginStatus;

public record LoginResponse(

        LoginStatus status,

        AuthResponse authentication,

        String challenge

) {
}