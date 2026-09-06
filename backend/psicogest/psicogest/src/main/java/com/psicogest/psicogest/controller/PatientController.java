package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
import com.psicogest.psicogest.dto.patient.PatientResponseDTO;
import com.psicogest.psicogest.dto.common.DeactivateDTO;
import com.psicogest.psicogest.service.PatientService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponseDTO create(
            @Valid @RequestBody PatientCreateDTO dto) {

        return patientService.create(dto);
    }

    @GetMapping
    public List<PatientResponseDTO> findAll() {

        return patientService.findAll();
    }

    @GetMapping("/{id}")
    public PatientResponseDTO findById(
            @PathVariable Long id) {

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
