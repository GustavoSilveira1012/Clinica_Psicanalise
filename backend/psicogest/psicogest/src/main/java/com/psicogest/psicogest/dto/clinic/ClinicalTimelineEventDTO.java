package com.psicogest.psicogest.dto.clinic;

import java.time.Instant;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.ClinicalTimelineEventType;

public record ClinicalTimelineEventDTO(

        UUID eventId,

        ClinicalTimelineEventType type,

        Instant occurredAt,

        Long patientId,

        Long psychoanalystId,

        String psychoanalystName,

        UUID medicalRecordId,

        Long appointmentId,

        UUID addendumId,

        String summary

) {
}