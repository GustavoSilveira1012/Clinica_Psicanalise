package com.psicogest.psicogest.exception;
public class InvalidMfaException extends RuntimeException {
    public InvalidMfaException() { super("Desafio ou código MFA inválido"); }
}
