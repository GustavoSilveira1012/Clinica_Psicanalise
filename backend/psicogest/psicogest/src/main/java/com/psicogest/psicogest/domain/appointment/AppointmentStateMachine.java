package com.psicogest.psicogest.domain.appointment;

import com.psicogest.psicogest.exception.InvalidAppointmentTransitionException;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class AppointmentStateMachine {

        private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS;

        static {

                Map<AppointmentStatus, Set<AppointmentStatus>> transitions = new EnumMap<>(
                                AppointmentStatus.class);

                /*
                 * Uma consulta não precisa obrigatoriamente
                 * passar por CONFIRMED para ser concluída.
                 *
                 * Isso evita transformar confirmação
                 * administrativa em requisito clínico.
                 */
                transitions.put(
                                AppointmentStatus.SCHEDULED,
                                EnumSet.of(
                                                AppointmentStatus.CONFIRMED,
                                                AppointmentStatus.COMPLETED,
                                                AppointmentStatus.NO_SHOW,
                                                AppointmentStatus.CANCELLED,
                                                AppointmentStatus.RESCHEDULED));

                transitions.put(
                                AppointmentStatus.CONFIRMED,
                                EnumSet.of(
                                                AppointmentStatus.COMPLETED,
                                                AppointmentStatus.NO_SHOW,
                                                AppointmentStatus.CANCELLED,
                                                AppointmentStatus.RESCHEDULED));

                /*
                 * Estados terminais.
                 */
                transitions.put(
                                AppointmentStatus.COMPLETED,
                                EnumSet.noneOf(
                                                AppointmentStatus.class));

                transitions.put(
                                AppointmentStatus.NO_SHOW,
                                EnumSet.noneOf(
                                                AppointmentStatus.class));

                transitions.put(
                                AppointmentStatus.CANCELLED,
                                EnumSet.noneOf(
                                                AppointmentStatus.class));

                transitions.put(
                                AppointmentStatus.RESCHEDULED,
                                EnumSet.noneOf(
                                                AppointmentStatus.class));

                ALLOWED_TRANSITIONS = Map.copyOf(transitions);
        }

        public void validateTransition(
                        AppointmentStatus current,
                        AppointmentStatus target) {

                if (current == null) {

                        throw new InvalidAppointmentTransitionException(
                                        "Status atual da consulta é inválido");
                }

                if (target == null) {

                        throw new InvalidAppointmentTransitionException(
                                        "Novo status da consulta é inválido");
                }

                if (current == target) {

                        throw new InvalidAppointmentTransitionException(
                                        "A consulta já possui o status "
                                                        + current);
                }

                Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.get(current);

                if (allowed == null
                                || !allowed.contains(target)) {

                        throw new InvalidAppointmentTransitionException(
                                        "Não é permitido alterar uma consulta de "
                                                        + current
                                                        + " para "
                                                        + target);
                }
        }

        public boolean canTransition(
                        AppointmentStatus current,
                        AppointmentStatus target) {

                if (current == null
                                || target == null) {
                        return false;
                }

                Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.get(current);

                return allowed != null
                                && allowed.contains(target);
        }
}