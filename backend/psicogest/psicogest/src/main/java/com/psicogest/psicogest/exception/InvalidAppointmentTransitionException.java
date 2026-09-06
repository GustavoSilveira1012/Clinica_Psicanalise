package com.psicogest.psicogest.exception;

public class InvalidAppointmentTransitionException
        extends RuntimeException {

    public InvalidAppointmentTransitionException(
            String message
    ) {
        super(message);
    }
}