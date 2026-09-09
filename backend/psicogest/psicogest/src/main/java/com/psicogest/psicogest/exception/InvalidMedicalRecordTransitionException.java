package com.psicogest.psicogest.exception;

public class InvalidMedicalRecordTransitionException
        extends RuntimeException {

    public InvalidMedicalRecordTransitionException(
            String message
    ) {
        super(message);
    }
}