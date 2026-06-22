package com.carebridge.backend.healthrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_record_id")
    private UUID healthRecordId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "record_type", length = 50)
    private String recordType;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
