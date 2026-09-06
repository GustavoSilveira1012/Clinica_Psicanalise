package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.psychoanalyst.PsychoanalystCreateDTO;
import com.psicogest.psicogest.dto.psychoanalyst.PsychoanalystResponseDTO;
import com.psicogest.psicogest.service.PsychoanalystService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/psychoanalysts")
public class PsychoanalystController {

    private final PsychoanalystService psychoanalystService;

    public PsychoanalystController(
            PsychoanalystService psychoanalystService) {
        this.psychoanalystService = psychoanalystService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PsychoanalystResponseDTO create(
            @Valid @RequestBody PsychoanalystCreateDTO dto) {

        return psychoanalystService.create(dto);
    }

    @GetMapping
    public List<PsychoanalystResponseDTO> findAll() {

        return psychoanalystService.findAll();
    }

    @GetMapping("/{id}")
    public PsychoanalystResponseDTO findById(
            @PathVariable Long id) {

        return psychoanalystService.findById(id);
    }
}