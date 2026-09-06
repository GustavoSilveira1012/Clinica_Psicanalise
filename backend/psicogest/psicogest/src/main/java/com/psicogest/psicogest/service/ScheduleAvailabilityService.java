package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.model.entity.Availability;
import com.psicogest.psicogest.model.entity.AvailabilityException;
import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;
import com.psicogest.psicogest.repository.AvailabilityExceptionRepository;
import com.psicogest.psicogest.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleAvailabilityService {

        private final AvailabilityRepository availabilityRepository;

        private final AvailabilityExceptionRepository exceptionRepository;

        public ScheduleAvailabilityService(
                        AvailabilityRepository availabilityRepository,
                        AvailabilityExceptionRepository exceptionRepository) {
                this.availabilityRepository = availabilityRepository;

                this.exceptionRepository = exceptionRepository;
        }

        @Transactional(readOnly = true)
        public boolean isWithinSchedule(
                        Long psychoanalystId,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime) {

                validateTime(
                                startTime,
                                endTime);

                List<AvailabilityException> exceptions = exceptionRepository
                                .findByPsychoanalystIdAndDateOrderByStartTimeAsc(
                                                psychoanalystId,
                                                date);

                /*
                 * 1. BLOCKED possui prioridade máxima.
                 */
                for (AvailabilityException exception : exceptions) {

                        if (exception.getType() != AvailabilityExceptionType.BLOCKED) {
                                continue;
                        }

                        if (isFullDay(exception)) {
                                return false;
                        }

                        if (overlaps(
                                        startTime,
                                        endTime,
                                        exception.getStartTime(),
                                        exception.getEndTime())) {
                                return false;
                        }
                }

                /*
                 * 2. Depois verificamos horários extras.
                 */
                for (AvailabilityException exception : exceptions) {

                        if (exception.getType() != AvailabilityExceptionType.EXTRA_AVAILABLE) {
                                continue;
                        }

                        if (contains(
                                        exception.getStartTime(),
                                        exception.getEndTime(),
                                        startTime,
                                        endTime)) {
                                return true;
                        }
                }

                /*
                 * 3. Por último, disponibilidade semanal.
                 */
                List<Availability> availabilities = availabilityRepository
                                .findByPsychoanalystIdAndDayOfWeekAndActiveTrue(
                                                psychoanalystId,
                                                date.getDayOfWeek());

                return availabilities
                                .stream()
                                .anyMatch(
                                                availability -> contains(
                                                                availability.getStartTime(),
                                                                availability.getEndTime(),
                                                                startTime,
                                                                endTime));
        }

        private boolean isFullDay(
                        AvailabilityException exception) {

                return exception.getStartTime() == null
                                && exception.getEndTime() == null;
        }

        private boolean contains(
                        LocalTime containerStart,
                        LocalTime containerEnd,
                        LocalTime requestedStart,
                        LocalTime requestedEnd) {

                boolean startsInside = !requestedStart.isBefore(containerStart);

                boolean endsInside = !requestedEnd.isAfter(containerEnd);

                return startsInside && endsInside;
        }

        private boolean overlaps(
                        LocalTime start1,
                        LocalTime end1,
                        LocalTime start2,
                        LocalTime end2) {

                return start1.isBefore(end2)
                                && end1.isAfter(start2);
        }

        private void validateTime(
                        LocalTime startTime,
                        LocalTime endTime) {

                if (startTime == null
                                || endTime == null
                                || !endTime.isAfter(startTime)) {

                        throw new InvalidAvailabilityException(
                                        "Intervalo de horário inválido");
                }
        }
}