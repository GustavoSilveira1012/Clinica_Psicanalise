package com.psicogest.psicogest.exception;

public class InvalidTherapeuticRelationshipTransitionException
        extends RuntimeException {

    public InvalidTherapeuticRelationshipTransitionException(
            String message
    ) {
        super(message);
    }
}