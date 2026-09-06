package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.clinic.ClinicMembershipCreateDTO;
import com.psicogest.psicogest.dto.clinic.ClinicMembershipResponseDTO;
import com.psicogest.psicogest.service.ClinicMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clinics/{clinicId}/members")
public class ClinicMembershipController {

    private final ClinicMembershipService membershipService;

    public ClinicMembershipController(
            ClinicMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicMembershipResponseDTO create(
            @PathVariable Long clinicId,
            @Valid @RequestBody ClinicMembershipCreateDTO dto) {

        return membershipService.create(clinicId, dto);
    }

    @GetMapping
    public List<ClinicMembershipResponseDTO> findByClinic(
            @PathVariable Long clinicId) {

        return membershipService.findByClinicId(clinicId);
    }
}