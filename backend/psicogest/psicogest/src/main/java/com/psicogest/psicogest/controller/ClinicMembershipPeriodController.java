package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.clinic.*;
import com.psicogest.psicogest.service.ClinicMembershipPeriodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clinic-memberships/{membershipId}/periods")
public class ClinicMembershipPeriodController {

        private final ClinicMembershipPeriodService periodService;

        public ClinicMembershipPeriodController(
                        ClinicMembershipPeriodService periodService) {

                this.periodService = periodService;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public ClinicMembershipPeriodResponseDTO start(
                        @PathVariable Long membershipId,
                        @RequestBody(required = false) ClinicMembershipPeriodCreateDTO dto) {

                if (dto == null) {

                        dto = new ClinicMembershipPeriodCreateDTO(
                                        null);
                }

                return periodService.start(
                                membershipId,
                                dto);
        }

        @GetMapping
        public List<ClinicMembershipPeriodResponseDTO> findAll(
                        @PathVariable Long membershipId) {

                return periodService.findAll(
                                membershipId);
        }

        @PatchMapping("/{periodId}/end")
        public ClinicMembershipPeriodResponseDTO end(
                        @PathVariable Long membershipId,
                        @PathVariable Long periodId,
                        @Valid @RequestBody ClinicMembershipPeriodEndDTO dto) {

                return periodService.end(
                                membershipId,
                                periodId,
                                dto);
        }
}