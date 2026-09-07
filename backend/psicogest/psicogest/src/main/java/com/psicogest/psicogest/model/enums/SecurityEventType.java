package com.psicogest.psicogest.model.enums;

public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    RATE_LIMIT_TRIGGERED,
    MFA_FAILURE,
    ACCESS_DENIED,
    REFRESH_TOKEN_REUSE,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,
    MFA_ENROLLED,
    SESSION_REVOKED,
    SECURITY_ALERT
}
