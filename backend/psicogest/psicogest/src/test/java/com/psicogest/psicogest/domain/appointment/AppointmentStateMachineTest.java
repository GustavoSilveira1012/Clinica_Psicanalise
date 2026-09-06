package com.psicogest.psicogest.domain.appointment;

import com.psicogest.psicogest.exception.InvalidAppointmentTransitionException;
import com.psicogest.psicogest.model.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentStateMachineTest {

        private final AppointmentStateMachine stateMachine = new AppointmentStateMachine();

        @Test
        void shouldAllowScheduledToConfirmed() {

                assertDoesNotThrow(
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.SCHEDULED,
                                                AppointmentStatus.CONFIRMED));
        }

        @Test
        void shouldAllowScheduledToCancelled() {

                assertDoesNotThrow(
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.SCHEDULED,
                                                AppointmentStatus.CANCELLED));
        }

        @Test
        void shouldAllowConfirmedToCompleted() {

                assertDoesNotThrow(
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.CONFIRMED,
                                                AppointmentStatus.COMPLETED));
        }

        @Test
        void shouldAllowScheduledToCompleted() {

                assertDoesNotThrow(
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.SCHEDULED,
                                                AppointmentStatus.COMPLETED));
        }

        @Test
        void shouldRejectCompletedToScheduled() {

                assertThrows(
                                InvalidAppointmentTransitionException.class,
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.COMPLETED,
                                                AppointmentStatus.SCHEDULED));
        }

        @Test
        void shouldRejectCancelledToConfirmed() {

                assertThrows(
                                InvalidAppointmentTransitionException.class,
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.CANCELLED,
                                                AppointmentStatus.CONFIRMED));
        }

        @Test
        void shouldRejectRescheduledToScheduled() {

                assertThrows(
                                InvalidAppointmentTransitionException.class,
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.RESCHEDULED,
                                                AppointmentStatus.SCHEDULED));
        }

        @Test
        void shouldRejectSameStatus() {

                assertThrows(
                                InvalidAppointmentTransitionException.class,
                                () -> stateMachine.validateTransition(
                                                AppointmentStatus.SCHEDULED,
                                                AppointmentStatus.SCHEDULED));
        }

        @Test
        void completedShouldBeTerminal() {

                assertFalse(
                                stateMachine.canTransition(
                                                AppointmentStatus.COMPLETED,
                                                AppointmentStatus.CONFIRMED));
        }

        @Test
        void cancelledShouldBeTerminal() {

                assertFalse(
                                stateMachine.canTransition(
                                                AppointmentStatus.CANCELLED,
                                                AppointmentStatus.SCHEDULED));
        }
}