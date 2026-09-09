package com.psicogest.psicogest.dto.medicalrecord;

import java.time.Instant;
import java.util.UUID;

/**
 * 17. DTO para listagem de revisões (metadados apenas, sem content)
 * 
 * Exemplo resposta:
 * [
 *   {
 *     "id": "...",
 *     "revisionNumber": 4,
 *     "authorPsychoanalystId": 10,
 *     "authorName": "Dr. João",
 *     "createdAt": "2026-09-09T18:30:00Z"
 *   },
 *   {
 *     "id": "...",
 *     "revisionNumber": 3,
 *     "authorPsychoanalystId": 10,
 *     "authorName": "Dr. João",
 *     "createdAt": "2026-09-09T18:25:00Z"
 *   }
 * ]
 */
public record MedicalRecordRevisionSummaryDTO(

        UUID id,

        Long revisionNumber,

        Long authorPsychoanalystId,

        String authorName,

        Instant createdAt

) {
}
