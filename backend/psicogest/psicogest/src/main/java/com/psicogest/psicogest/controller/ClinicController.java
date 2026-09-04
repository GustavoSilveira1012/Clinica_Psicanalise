package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.clinic.ClinicCreateDTO;
import com.psicogest.psicogest.dto.clinic.ClinicResponseDTO;
import com.psicogest.psicogest.service.ClinicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/clinics")
public class ClinicController {
    private final ClinicService clinicService;

    public ClinicController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponseDTO create(
            @Valid @RequestBody ClinicCreateDTO dto
    ) {
        return clinicService.create(dto);
    }

    @GetMapping
    public List<ClinicResponseDTO> findAll() {
        return clinicService.findAll();
    }

    @GetMapping("/{id}")
    public ClinicResponseDTO findById(
            @PathVariable Long id
    ) {
        return clinicService.findById(id);
    }
}
