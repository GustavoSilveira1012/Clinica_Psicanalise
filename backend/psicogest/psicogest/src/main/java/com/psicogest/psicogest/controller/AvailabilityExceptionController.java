package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.availability.AvailabilityExceptionCreateDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityExceptionResponseDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityExceptionUpdateDTO;
import com.psicogest.psicogest.service.AvailabilityExceptionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/psychoanalysts/{psychoanalystId}/availability-exceptions")
public class AvailabilityExceptionController {

        private final AvailabilityExceptionService exceptionService;

        public AvailabilityExceptionController(
                        AvailabilityExceptionService exceptionService) {
                this.exceptionService = exceptionService;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public AvailabilityExceptionResponseDTO create(
                        @PathVariable Long psychoanalystId,
                        @Valid @RequestBody AvailabilityExceptionCreateDTO dto) {

                return exceptionService.create(
                                psychoanalystId,
                                dto);
        }

        @GetMapping
        public List<AvailabilityExceptionResponseDTO> findAll(
                        @PathVariable Long psychoanalystId,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

                return exceptionService.findAll(
                                psychoanalystId,
                                from,
                                to);
        }

        @GetMapping("/{exceptionId}")
        public AvailabilityExceptionResponseDTO findById(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long exceptionId) {

                return exceptionService.findById(
                                psychoanalystId,
                                exceptionId);
        }

        @PutMapping("/{exceptionId}")
        public AvailabilityExceptionResponseDTO update(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long exceptionId,
                        @Valid @RequestBody AvailabilityExceptionUpdateDTO dto) {

                return exceptionService.update(
                                psychoanalystId,
                                exceptionId,
                                dto);
        }

        @DeleteMapping("/{exceptionId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long exceptionId) {

                exceptionService.delete(
                                psychoanalystId,
                                exceptionId);
        }
}