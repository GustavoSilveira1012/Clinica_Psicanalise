package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
import com.psicogest.psicogest.dto.patient.PatientUpdateDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

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
    @PreAuthorize("hasAnyRole('CLINIC_ADMIN', 'PSYCHOANALYST')")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponseDTO create(
            @Valid @RequestBody PatientCreateDTO dto) {

        return patientService.create(dto);
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyRole(
                'PATIENT',
                'PSYCHOANALYST',
                'CLINIC_ADMIN'
            )
            """)
    public List<PatientResponseDTO> findAll(Authentication authentication) {
        return patientAccessQueryService.findAccessible(authentication);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PATIENT', 'PSYCHOANALYST', 'CLINIC_ADMIN')")
    public Page<PatientResponseDTO> search(
            Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 25, sort = "id") Pageable pageable
    ) {
        return patientAccessQueryService.search(authentication, query, active, pageable);
    }

    @GetMapping("/{id}")
@PreAuthorize("""
        @clinicalAuthorizationService.canReadPatientProfile(
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
    @PreAuthorize("hasAnyRole('CLINIC_ADMIN', 'PSYCHOANALYST')")
    public PatientResponseDTO deactivate(
            @PathVariable Long id,
            @Valid @RequestBody DeactivateDTO dto) {
        return patientService.deactivate(id, dto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@clinicalAuthorizationService.canWritePatientProfile(authentication, #id)")
    public PatientResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody PatientUpdateDTO dto
    ) {
        return patientService.update(id, dto);
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('CLINIC_ADMIN')")
    public PatientResponseDTO reactivate(@PathVariable Long id) {
        return patientService.reactivate(id);
    }
}
