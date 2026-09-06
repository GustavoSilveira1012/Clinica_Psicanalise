package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.relationship.TherapeuticRelationshipResponseDTO;
import com.psicogest.psicogest.service.TherapeuticRelationshipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/patients/{patientId}/therapeutic-relationships"
)
public class PatientRelationshipController {

    private final TherapeuticRelationshipService
            relationshipService;

    public PatientRelationshipController(
            TherapeuticRelationshipService relationshipService
    ) {

        this.relationshipService =
                relationshipService;
    }

    @GetMapping
    public List<TherapeuticRelationshipResponseDTO>
    findByPatient(
            @PathVariable Long patientId
    ) {

        return relationshipService
                .findByPatient(
                        patientId
                );
    }
}