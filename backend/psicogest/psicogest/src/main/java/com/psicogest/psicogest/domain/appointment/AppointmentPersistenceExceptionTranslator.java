package com.psicogest.psicogest.domain.appointment;

import com.psicogest.psicogest.exception.ScheduleConflictException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AppointmentPersistenceExceptionTranslator {

    private static final String OVERLAP_CONSTRAINT =
            "ex_appointments_psychoanalyst_no_overlap";

    public RuntimeException translate(
            DataIntegrityViolationException exception
    ) {

        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof PSQLException postgresException) {

                String constraint =
                        postgresException
                                .getServerErrorMessage() != null
                                ? postgresException
                                    .getServerErrorMessage()
                                    .getConstraint()
                                : null;

                if (
                        OVERLAP_CONSTRAINT.equals(
                                constraint
                        )
                ) {

                    return new ScheduleConflictException(
                            "O horário acabou de ser ocupado por outra consulta"
                    );
                }
            }

            cause = cause.getCause();
        }

        return exception;
    }
}