package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.relationship.*;
import com.psicogest.psicogest.service.TherapeuticRelationshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/psychoanalysts/{psychoanalystId}/therapeutic-relationships")
public class TherapeuticRelationshipController {

        private final TherapeuticRelationshipService relationshipService;

        public TherapeuticRelationshipController(
                        TherapeuticRelationshipService relationshipService) {

                this.relationshipService = relationshipService;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public TherapeuticRelationshipResponseDTO create(
                        @PathVariable Long psychoanalystId,
                        @Valid @RequestBody TherapeuticRelationshipCreateDTO dto) {

                return relationshipService.create(
                                psychoanalystId,
                                dto);
        }

        @GetMapping
        public List<TherapeuticRelationshipResponseDTO> findByPsychoanalyst(
                        @PathVariable Long psychoanalystId) {

                return relationshipService
                                .findByPsychoanalyst(
                                                psychoanalystId);
        }

        @PatchMapping("/{relationshipId}/suspend")
        public TherapeuticRelationshipResponseDTO suspend(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long relationshipId) {

                return relationshipService.suspend(
                                psychoanalystId,
                                relationshipId);
        }

        @PatchMapping("/{relationshipId}/resume")
        public TherapeuticRelationshipResponseDTO resume(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long relationshipId) {

                return relationshipService.resume(
                                psychoanalystId,
                                relationshipId);
        }

        @PatchMapping("/{relationshipId}/primary")
        public TherapeuticRelationshipResponseDTO makePrimary(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long relationshipId) {

                return relationshipService.makePrimary(
                                psychoanalystId,
                                relationshipId);
        }

        @PatchMapping("/{relationshipId}/end")
        public TherapeuticRelationshipResponseDTO end(
                        @PathVariable Long psychoanalystId,
                        @PathVariable Long relationshipId,
                        @Valid @RequestBody TherapeuticRelationshipEndDTO dto) {

                return relationshipService.end(
                                psychoanalystId,
                                relationshipId,
                                dto);
        }
}