package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.availability.AvailabilityCreateDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityResponseDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityUpdateDTO;
import com.psicogest.psicogest.exception.InvalidAvailabilityException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.exception.ScheduleConflictException;
import com.psicogest.psicogest.model.entity.Availability;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.repository.AvailabilityRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class AvailabilityService {

        private final AvailabilityRepository availabilityRepository;
        private final PsychoanalystRepository psychoanalystRepository;

        public AvailabilityService(
                        AvailabilityRepository availabilityRepository,
                        PsychoanalystRepository psychoanalystRepository) {
                this.availabilityRepository = availabilityRepository;
                this.psychoanalystRepository = psychoanalystRepository;
        }

        @Transactional
        public AvailabilityResponseDTO create(
                        Long psychoanalystId,
                        AvailabilityCreateDTO dto) {

                validateTime(dto.startTime(), dto.endTime());

                validateOverlap(
                                psychoanalystId,
                                dto.dayOfWeek(),
                                dto.startTime(),
                                dto.endTime(),
                                null,
                                true);

                Psychoanalyst psychoanalyst = findPsychoanalystById(psychoanalystId);

                Availability availability = Availability.builder()
                                .psychoanalyst(psychoanalyst)
                                .dayOfWeek(dto.dayOfWeek())
                                .startTime(dto.startTime())
                                .endTime(dto.endTime())
                                .active(true)
                                .build();

                Availability savedAvailability = availabilityRepository.save(availability);

                return toResponseDTO(savedAvailability);
        }

        public List<AvailabilityResponseDTO> findByPsychoanalystId(
                        Long psychoanalystId) {

                findPsychoanalystById(psychoanalystId);

                return availabilityRepository
                                .findByPsychoanalystId(psychoanalystId)
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        public AvailabilityResponseDTO findById(Long id) {

                Availability availability = findAvailabilityById(id, id);

                return toResponseDTO(availability);
        }

        @Transactional
        public AvailabilityResponseDTO update(
                        Long psychoanalystId,
                        Long availabilityId,
                        AvailabilityUpdateDTO dto) {

                validateTime(dto.startTime(), dto.endTime());

                Availability availability = findAvailabilityById(
                                psychoanalystId,
                                availabilityId);

                validateOverlap(
                                psychoanalystId,
                                dto.dayOfWeek(),
                                dto.startTime(),
                                dto.endTime(),
                                availabilityId,
                                dto.active());

                availability.setDayOfWeek(dto.dayOfWeek());
                availability.setStartTime(dto.startTime());
                availability.setEndTime(dto.endTime());
                availability.setActive(dto.active());

                Availability updatedAvailability = availabilityRepository.save(availability);

                return toResponseDTO(updatedAvailability);
        }

        @Transactional
        public void delete(Long psychoanalystId, Long availabilityId) {

                Availability availability = findAvailabilityById(
                                psychoanalystId,
                                availabilityId);

                availabilityRepository.delete(availability);
        }

        private Psychoanalyst findPsychoanalystById(
                        Long psychoanalystId) {

                return psychoanalystRepository.findById(psychoanalystId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Psicanalista não encontrado com o ID: "
                                                                + psychoanalystId));
        }

        private Availability findAvailabilityById(
                        Long psychoanalystId,
                        Long availabilityId) {

                return availabilityRepository
                                .findByIdAndPsychoanalystId(
                                                availabilityId,
                                                psychoanalystId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Disponibilidade não encontrada"));
        }

        private void validateTime(
                        LocalTime startTime,
                        LocalTime endTime) {

                if (!endTime.isAfter(startTime)) {
                        throw new InvalidAvailabilityException(
                                        "O horário final deve ser posterior ao horário inicial");
                }
        }

        private AvailabilityResponseDTO toResponseDTO(
                        Availability availability) {

                return new AvailabilityResponseDTO(
                                availability.getId(),
                                availability.getPsychoanalyst().getId(),
                                availability.getDayOfWeek(),
                                availability.getStartTime(),
                                availability.getEndTime(),
                                availability.getActive());
        }

        private void validateOverlap(
                        Long psychoanalystId,
                        DayOfWeek dayOfWeek,
                        LocalTime startTime,
                        LocalTime endTime,
                        Long ignoredId,
                        boolean active) {

                if (!active) {
                        return;
                }

                List<Availability> existingAvailabilities = availabilityRepository
                                .findByPsychoanalystIdAndDayOfWeekAndActiveTrue(
                                                psychoanalystId,
                                                dayOfWeek);

                for (Availability existing : existingAvailabilities) {

                        if (ignoredId != null
                                        && existing.getId().equals(ignoredId)) {
                                continue;
                        }

                        boolean overlaps = startTime.isBefore(existing.getEndTime())
                                        && endTime.isAfter(
                                                        existing.getStartTime());

                        if (overlaps) {

                                throw new ScheduleConflictException(
                                                "Já existe uma disponibilidade ativa que conflita com este horário");
                        }
                }
        }

        public AvailabilityResponseDTO findById(Long psychoanalystId, Long availabilityId) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findById'");
        }
}