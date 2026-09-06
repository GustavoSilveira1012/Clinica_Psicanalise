package com.psicogest.psicogest.exception;

public class InvalidAppointmentSeriesException
        extends RuntimeException {

    public InvalidAppointmentSeriesException(
            String message
    ) {
        super(message);
    }
}