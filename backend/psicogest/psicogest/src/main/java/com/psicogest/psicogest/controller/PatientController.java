package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
import com.psicogest.psicogest.dto.patient.PatientResponseDTO;
import com.psicogest.psicogest.dto.common.DeactivateDTO;
import com.psicogest.psicogest.service.PatientService;
import com.psicogest.psicogest.service.PatientAccessQueryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {
    private final PatientService patientService;
    private final PatientAccessQueryService patientAccessQueryService;

    public PatientController(
            PatientService patientService,
            PatientAccessQueryService patientAccessQueryService
    ) {
        this.patientService = patientService;
        this.patientAccessQueryService = patientAccessQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponseDTO create(
            @Valid @RequestBody PatientCreateDTO dto) {

        return patientService.create(dto);
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyRole(
                'PATIENT',
                'PSYCHOANALYST'
            )
            """)
    public List<PatientResponseDTO> findAll(Authentication authentication) {
        return patientAccessQueryService.findAccessible(authentication);
    }

    @GetMapping("/{id}")
@PreAuthorize("""
        @clinicalAuthorization.canReadPatientProfile(
            authentication,
            #id
        )
        """)
public PatientResponseDTO findById(
        @PathVariable Long id
) {

    return patientService.findById(id);
}

    @PatchMapping("/{id}/deactivate")
    public PatientResponseDTO deactivate(
            @PathVariable Long id,
            @Valid @RequestBody DeactivateDTO dto) {
        return patientService.deactivate(id, dto);
    }

    @PatchMapping("/{id}/reactivate")
    public PatientResponseDTO reactivate(@PathVariable Long id) {
        return patientService.reactivate(id);
    }
}
