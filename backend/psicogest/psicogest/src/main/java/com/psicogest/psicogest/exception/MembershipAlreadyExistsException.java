package com.psicogest.psicogest.exception;

public class MembershipAlreadyExistsException extends RuntimeException {

    public MembershipAlreadyExistsException(String message) {
        super(message);
    }
}