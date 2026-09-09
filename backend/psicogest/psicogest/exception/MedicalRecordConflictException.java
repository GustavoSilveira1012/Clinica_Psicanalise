package com.psicogest.psicogest.exception;

public class MedicalRecordConflictException extends RuntimeException {

    public MedicalRecordConflictException( String message ) {
        super( message );
    }

    public MedicalRecordConflictException( String message, Throwable cause ) {
        super( message, cause );
    }
}
