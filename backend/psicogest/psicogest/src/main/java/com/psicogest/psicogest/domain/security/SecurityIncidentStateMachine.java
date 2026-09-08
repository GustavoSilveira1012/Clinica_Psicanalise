package com.psicogest.psicogest.domain.security;

import com.psicogest.psicogest.model.enums.SecurityIncidentStatus;
import org.springframework.stereotype.Component;

@Component
public class SecurityIncidentStateMachine {

    public boolean canTransition(
            SecurityIncidentStatus from,
            SecurityIncidentStatus to
    ) {

        return switch ( from ) {

            case OPEN ->
                    to == SecurityIncidentStatus.INVESTIGATING
                    ||
                    to == SecurityIncidentStatus.FALSE_POSITIVE;

            case INVESTIGATING ->
                    to == SecurityIncidentStatus.CONTAINED
                    ||
                    to == SecurityIncidentStatus.FALSE_POSITIVE;

            case CONTAINED ->
                    to == SecurityIncidentStatus.ERADICATING;

            case ERADICATING ->
                    to == SecurityIncidentStatus.RECOVERING;

            case RECOVERING ->
                    to == SecurityIncidentStatus.RESOLVED;

            case RESOLVED,
                 FALSE_POSITIVE ->
                    false;
        };
    }

    public void assertTransitionValid(
            SecurityIncidentStatus from,
            SecurityIncidentStatus to
    ) {

        if ( !canTransition( from, to ) ) {

            throw new IllegalStateException(
                    "Transição inválida de "
                    + from
                    + " para "
                    + to
            );
        }
    }
}
