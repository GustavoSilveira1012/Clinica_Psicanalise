package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.availability.AvailabilityCreateDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityResponseDTO;
import com.psicogest.psicogest.dto.availability.AvailabilityUpdateDTO;
import com.psicogest.psicogest.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/psychoanalysts/{psychoanalystId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(
            AvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityResponseDTO create(
            @PathVariable Long psychoanalystId,
            @Valid @RequestBody AvailabilityCreateDTO dto
    ) {

        return availabilityService.create(
                psychoanalystId,
                dto
        );
    }

    @GetMapping
    public List<AvailabilityResponseDTO> findByPsychoanalyst(
            @PathVariable Long psychoanalystId
    ) {

        return availabilityService.findByPsychoanalystId(
                psychoanalystId
        );
    }

    @GetMapping("/{availabilityId}")
    public AvailabilityResponseDTO findById(
            @PathVariable Long psychoanalystId,
            @PathVariable Long availabilityId
    ) {

        return availabilityService.findById(
                psychoanalystId,
                availabilityId
        );
    }

    @PutMapping("/{availabilityId}")
    public AvailabilityResponseDTO update(
            @PathVariable Long psychoanalystId,
            @PathVariable Long availabilityId,
            @Valid @RequestBody AvailabilityUpdateDTO dto
    ) {

        return availabilityService.update(
                psychoanalystId,
                availabilityId,
                dto
        );
    }

    @DeleteMapping("/{availabilityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long psychoanalystId,
            @PathVariable Long availabilityId
    ) {

        availabilityService.delete(
                psychoanalystId,
                availabilityId
        );
    }
}