package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.repository.projection.ClinicalTimelineProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 8. Repository dedicado para timeline clínica
 * 
 * Usa EntityManager pois combina múltiplas entities
 * não pertence naturalmente a uma única entity.
 * 
 * 9. Query: metadata only (sem encrypted_content)
 */
@Slf4j
@Repository
public class ClinicalTimelineRepository {

    private final EntityManager entityManager;

    public ClinicalTimelineRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Busca timeline clínica unificada de um paciente
     * 
     * Combina:
     * - APPOINTMENT_COMPLETED
     * - MEDICAL_RECORD_FINALIZED
     * - MEDICAL_RECORD_ADDENDUM_CREATED
     * 
     * Ordenado por data DESC (mais recente primeiro)
     * Com paginação
     */
    public Page<ClinicalTimelineProjection> findByPatientId(
            Long patientId,
            Pageable pageable
    ) {
        // 9. Query: metadata only - sem encrypted_content
        String sql = """
                SELECT
                    CAST(a.id AS text) AS event_id,
                    'APPOINTMENT_COMPLETED' AS event_type,
                    a.scheduled_end AS occurred_at,
                    a.patient_id,
                    a.psychoanalyst_id,
                    CAST(NULL AS text) AS medical_record_id,
                    a.id AS appointment_id,
                    CAST(NULL AS text) AS addendum_id,
                    u.name AS psychoanalyst_name
                FROM appointments a
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = a.psychoanalyst_id
                )
                WHERE a.patient_id = :patientId
                AND a.status = 'COMPLETED'
                
                UNION ALL
                
                SELECT
                    CAST(m.id AS text),
                    'MEDICAL_RECORD_FINALIZED',
                    m.finalized_at,
                    m.patient_id,
                    m.author_psychoanalyst_id,
                    CAST(m.id AS text),
                    m.appointment_id,
                    CAST(NULL AS text),
                    u.name
                FROM medical_records m
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = m.author_psychoanalyst_id
                )
                WHERE m.patient_id = :patientId
                AND m.status = 'FINALIZED'
                
                UNION ALL
                
                SELECT
                    CAST(ad.id AS text),
                    'MEDICAL_RECORD_ADDENDUM_CREATED',
                    ad.created_at,
                    m.patient_id,
                    ad.author_psychoanalyst_id,
                    CAST(m.id AS text),
                    m.appointment_id,
                    CAST(ad.id AS text),
                    u.name
                FROM medical_record_addendums ad
                JOIN medical_records m ON m.id = ad.medical_record_id
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = ad.author_psychoanalyst_id
                )
                WHERE m.patient_id = :patientId
                
                ORDER BY occurred_at DESC
                """;

        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM (" + sql + ") AS timeline"
        );
        countQuery.setParameter("patientId", patientId);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        Query query = entityManager.createNativeQuery(
                sql,
                ClinicalTimelineProjection.class
        );
        query.setParameter("patientId", patientId);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<ClinicalTimelineProjection> content = query.getResultList();

        log.info("Timeline clínica carregada: patientId={}, eventos={}, página={}/{}",
                patientId, content.size(), pageable.getPageNumber(), 
                (total + pageable.getPageSize() - 1) / pageable.getPageSize());

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Busca timeline com filtro de data
     * (para exportação)
     */
    public Page<ClinicalTimelineProjection> findByPatientIdAndDateRange(
            Long patientId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        LocalDateTime fromDateTime = from != null 
            ? from.atStartOfDay() 
            : null;
        LocalDateTime toDateTime = to != null 
            ? to.plusDays(1).atStartOfDay() 
            : null;

        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT
                    CAST(a.id AS text) AS event_id,
                    'APPOINTMENT_COMPLETED' AS event_type,
                    a.scheduled_end AS occurred_at,
                    a.patient_id,
                    a.psychoanalyst_id,
                    CAST(NULL AS text) AS medical_record_id,
                    a.id AS appointment_id,
                    CAST(NULL AS text) AS addendum_id,
                    u.name AS psychoanalyst_name
                FROM appointments a
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = a.psychoanalyst_id
                )
                WHERE a.patient_id = :patientId
                AND a.status = 'COMPLETED'
                """);

        if (fromDateTime != null) {
            sql.append(" AND a.scheduled_end >= :fromDateTime ");
        }
        if (toDateTime != null) {
            sql.append(" AND a.scheduled_end < :toDateTime ");
        }

        sql.append("""
                UNION ALL
                
                SELECT
                    CAST(m.id AS text),
                    'MEDICAL_RECORD_FINALIZED',
                    m.finalized_at,
                    m.patient_id,
                    m.author_psychoanalyst_id,
                    CAST(m.id AS text),
                    m.appointment_id,
                    CAST(NULL AS text),
                    u.name
                FROM medical_records m
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = m.author_psychoanalyst_id
                )
                WHERE m.patient_id = :patientId
                AND m.status = 'FINALIZED'
                """);

        if (fromDateTime != null) {
            sql.append(" AND m.finalized_at >= :fromDateTime ");
        }
        if (toDateTime != null) {
            sql.append(" AND m.finalized_at < :toDateTime ");
        }

        sql.append("""
                UNION ALL
                
                SELECT
                    CAST(ad.id AS text),
                    'MEDICAL_RECORD_ADDENDUM_CREATED',
                    ad.created_at,
                    m.patient_id,
                    ad.author_psychoanalyst_id,
                    CAST(m.id AS text),
                    m.appointment_id,
                    CAST(ad.id AS text),
                    u.name
                FROM medical_record_addendums ad
                JOIN medical_records m ON m.id = ad.medical_record_id
                JOIN users u ON u.id = (
                    SELECT user_id FROM psychoanalysts 
                    WHERE id = ad.author_psychoanalyst_id
                )
                WHERE m.patient_id = :patientId
                """);

        if (fromDateTime != null) {
            sql.append(" AND ad.created_at >= :fromDateTime ");
        }
        if (toDateTime != null) {
            sql.append(" AND ad.created_at < :toDateTime ");
        }

        sql.append(" ORDER BY occurred_at DESC ");

        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM (" + sql.toString() + ") AS timeline"
        );
        countQuery.setParameter("patientId", patientId);
        if (fromDateTime != null) countQuery.setParameter("fromDateTime", fromDateTime);
        if (toDateTime != null) countQuery.setParameter("toDateTime", toDateTime);
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        Query query = entityManager.createNativeQuery(
                sql.toString(),
                ClinicalTimelineProjection.class
        );
        query.setParameter("patientId", patientId);
        if (fromDateTime != null) query.setParameter("fromDateTime", fromDateTime);
        if (toDateTime != null) query.setParameter("toDateTime", toDateTime);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<ClinicalTimelineProjection> content = query.getResultList();

        return new PageImpl<>(content, pageable, total);
    }
}
