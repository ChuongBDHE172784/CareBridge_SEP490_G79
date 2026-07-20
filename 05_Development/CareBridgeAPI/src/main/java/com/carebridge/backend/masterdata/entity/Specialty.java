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

@Entity
@Table(name = "specialties")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specialty {
 @Id
 @Column(name = "specialty_id", length = 5, nullable = false, updatable = false)
 private String specialtyId;

 @Column(name = "name", length = 100, nullable = false)
 private String name;

 @Column(name = "description", columnDefinition = "text")
 private String description;

 @Column(name = "category", length = 50)
 private String category;

 @Column(name = "is_active", nullable = false)
 private Boolean isActive = true;
}
