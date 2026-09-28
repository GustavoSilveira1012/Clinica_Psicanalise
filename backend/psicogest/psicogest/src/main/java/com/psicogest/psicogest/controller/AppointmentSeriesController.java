package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.appointment.*;
import com.psicogest.psicogest.service.AppointmentSeriesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/psychoanalysts/{psychoanalystId}/appointment-series")
public class AppointmentSeriesController {
        private final AppointmentSeriesService seriesService;

        public AppointmentSeriesController(
                        AppointmentSeriesService seriesService) {
                this.seriesService = seriesService;
        }

        @PostMapping
        @PreAuthorize("@identityAuthorization.isPsychoanalystSelf(authentication, #psychoanalystId)")
        @ResponseStatus(HttpStatus.CREATED)
        public AppointmentSeriesResponseDTO create(
        @PathVariable Long psychoanalystId,
                        @Valid @RequestBody AppointmentSeriesCreateDTO dto) {

                return seriesService.create(
                                psychoanalystId,
                                dto);
        }
}
