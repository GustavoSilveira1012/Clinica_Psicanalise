package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "package_plan_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackagePlanItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_plan_version_id", nullable = false, updatable = false)
    private PackagePlanVersion packagePlanVersion;

    @Column(name = "service_code", nullable = false, length = 100)
    private String serviceCode;

    @Column(name = "appointment_type", length = 30)
    private String appointmentType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
