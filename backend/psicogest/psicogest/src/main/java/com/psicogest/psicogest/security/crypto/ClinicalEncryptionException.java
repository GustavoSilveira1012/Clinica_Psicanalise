package com.psicogest.psicogest.security.crypto;

public class ClinicalEncryptionException extends RuntimeException {

    public ClinicalEncryptionException( String message ) {
        super( message );
    }

    public ClinicalEncryptionException( String message, Throwable cause ) {
        super( message, cause );
    }
}
