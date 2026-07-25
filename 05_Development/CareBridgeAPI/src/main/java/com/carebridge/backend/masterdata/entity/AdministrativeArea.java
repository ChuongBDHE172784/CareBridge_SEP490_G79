package com.carebridge.backend.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "administrative_areas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeArea {

    @Id
    @Column(name = "administrative_area_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "parent_area_id")
    private UUID parentAreaId;

    @Column(name = "area_type", nullable = false, length = 30)
    private String areaType;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "legacy_code", length = 80)
    private String legacyCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
