package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.availability.AvailabilityExceptionCreateDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityExceptionResponseDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityExceptionUpdateDTO;
import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.ScheduleConflictException;
import com.psicogest.psicogest.model.entity.AvailabilityException;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;
import com.psicogest.psicogest.repository.AvailabilityExceptionRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AvailabilityExceptionService {
        private final AvailabilityExceptionRepository exceptionRepository;
        private final PsychoanalystRepository psychoanalystRepository;

        public AvailabilityExceptionService(
                        AvailabilityExceptionRepository exceptionRepository,
                        PsychoanalystRepository psychoanalystRepository) {
                this.exceptionRepository = exceptionRepository;
                this.psychoanalystRepository = psychoanalystRepository;
        }

        @Transactional
        public AvailabilityExceptionResponseDTO create(
                        Long psychoanalystId,
                        AvailabilityExceptionCreateDTO dto) {

                validate(
                                dto.type(),
                                dto.startTime(),
                                dto.endTime());

                Psychoanalyst psychoanalyst = findPsychoanalystById(psychoanalystId);

                validateConflict(
                                psychoanalystId,
                                dto.date(),
                                dto.startTime(),
                                dto.endTime(),
                                null);

                AvailabilityException exception = AvailabilityException.builder()
                                .psychoanalyst(psychoanalyst)
                                .date(dto.date())
                                .type(dto.type())
                                .startTime(dto.startTime())
                                .endTime(dto.endTime())
                                .reason(normalizeReason(dto.reason()))
                                .build();

                return toResponseDTO(
                                exceptionRepository.save(exception));
        }

        @Transactional(readOnly = true)
        public List<AvailabilityExceptionResponseDTO> findAll(
                        Long psychoanalystId,
                        LocalDate from,
                        LocalDate to) {

                findPsychoanalystById(psychoanalystId);

                if ((from == null) != (to == null)) {
                        throw new InvalidAvailabilityException(
                                        "Os parâmetros 'from' e 'to' devem ser informados juntos");
                }

                if (from != null && to.isBefore(from)) {
                        throw new InvalidAvailabilityException(
                                        "A data final não pode ser anterior à data inicial");
                }

                List<AvailabilityException> exceptions;

                if (from != null) {

                        exceptions = exceptionRepository
                                        .findByPsychoanalystIdAndDateBetweenOrderByDateAscStartTimeAsc(
                                                        psychoanalystId,
                                                        from,
                                                        to);

                } else {

                        exceptions = exceptionRepository
                                        .findByPsychoanalystIdOrderByDateAscStartTimeAsc(
                                                        psychoanalystId);
                }

                return exceptions
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        @Transactional(readOnly = true)
        public AvailabilityExceptionResponseDTO findById(
                        Long psychoanalystId,
                        Long exceptionId) {

                return toResponseDTO(
                                findExceptionById(
                                                psychoanalystId,
                                                exceptionId));
        }

        @Transactional
        public AvailabilityExceptionResponseDTO update(
                        Long psychoanalystId,
                        Long exceptionId,
                        AvailabilityExceptionUpdateDTO dto) {

                AvailabilityException exception = findExceptionById(
                                psychoanalystId,
                                exceptionId);

                validate(
                                dto.type(),
                                dto.startTime(),
                                dto.endTime());

                validateConflict(
                                psychoanalystId,
                                dto.date(),
                                dto.startTime(),
                                dto.endTime(),
                                exceptionId);

                exception.setDate(dto.date());
                exception.setType(dto.type());
                exception.setStartTime(dto.startTime());
                exception.setEndTime(dto.endTime());
                exception.setReason(
                                normalizeReason(dto.reason()));

                return toResponseDTO(
                                exceptionRepository.save(exception));
        }

        @Transactional
        public void delete(
                        Long psychoanalystId,
                        Long exceptionId) {

                AvailabilityException exception = findExceptionById(
                                psychoanalystId,
                                exceptionId);

                exceptionRepository.delete(exception);
        }

        private void validate(
                        AvailabilityExceptionType type,
                        LocalTime startTime,
                        LocalTime endTime) {

                boolean startProvided = startTime != null;
                boolean endProvided = endTime != null;

                if (startProvided != endProvided) {

                        throw new InvalidAvailabilityException(
                                        "Horário inicial e horário final devem ser informados juntos");
                }

                if (startProvided && !endTime.isAfter(startTime)) {

                        throw new InvalidAvailabilityException(
                                        "O horário final deve ser posterior ao horário inicial");
                }

                if (type == AvailabilityExceptionType.EXTRA_AVAILABLE
                                && !startProvided) {

                        throw new InvalidAvailabilityException(
                                        "Disponibilidade extra exige horário inicial e final");
                }
        }

        private void validateConflict(
                        Long psychoanalystId,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime,
                        Long ignoredId) {

                List<AvailabilityException> existingExceptions = exceptionRepository
                                .findByPsychoanalystIdAndDateOrderByStartTimeAsc(
                                                psychoanalystId,
                                                date);

                for (AvailabilityException existing : existingExceptions) {

                        if (ignoredId != null
                                        && existing.getId().equals(ignoredId)) {
                                continue;
                        }

                        if (overlaps(
                                        startTime,
                                        endTime,
                                        existing.getStartTime(),
                                        existing.getEndTime())) {

                                throw new ScheduleConflictException(
                                                "Já existe uma exceção de agenda conflitante nesta data e horário");
                        }
                }
        }

        private boolean overlaps(
                        LocalTime start1,
                        LocalTime end1,
                        LocalTime start2,
                        LocalTime end2) {

                // null/null representa o dia inteiro.
                if (start1 == null || start2 == null) {
                        return true;
                }

                return start1.isBefore(end2)
                                && end1.isAfter(start2);
        }

        private Psychoanalyst findPsychoanalystById(
                        Long psychoanalystId) {

                return psychoanalystRepository
                                .findById(psychoanalystId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Psicanalista não encontrado com o ID: "
                                                                + psychoanalystId));
        }

        private AvailabilityException findExceptionById(
                        Long psychoanalystId,
                        Long exceptionId) {

                return exceptionRepository
                                .findByIdAndPsychoanalystId(
                                                exceptionId,
                                                psychoanalystId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Exceção de disponibilidade não encontrada"));
        }

        private String normalizeReason(String reason) {

                if (reason == null || reason.isBlank()) {
                        return null;
                }

                return reason.trim();
        }

        private AvailabilityExceptionResponseDTO toResponseDTO(
                        AvailabilityException exception) {

                boolean fullDay = exception.getStartTime() == null
                                && exception.getEndTime() == null;

                return new AvailabilityExceptionResponseDTO(
                                exception.getId(),
                                exception.getPsychoanalyst().getId(),
                                exception.getDate(),
                                exception.getType(),
                                exception.getStartTime(),
                                exception.getEndTime(),
                                fullDay,
                                exception.getReason());
        }
}