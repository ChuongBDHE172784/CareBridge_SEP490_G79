package com.carebridge.backend.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "specialties")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specialty {
 @Id
 @Column(name = "specialty_id", nullable = false, updatable = false)
 private UUID specialtyId;

 @Column(name = "code", length = 80, nullable = false, unique = true)
 private String code;

 @Column(name = "name", length = 150, nullable = false)
 private String name;

 @Column(name = "description", columnDefinition = "text")
 private String description;

 @Column(name = "is_active", nullable = false)
 @Builder.Default
 private Boolean isActive = true;

 @CreationTimestamp
 @Column(name = "created_at", nullable = false, updatable = false)
 private LocalDateTime createdAt;
}
