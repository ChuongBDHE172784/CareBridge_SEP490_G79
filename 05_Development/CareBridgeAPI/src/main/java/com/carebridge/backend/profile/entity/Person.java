package com.carebridge.backend.profile.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "persons")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Person {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "person_id")
    private UUID id;
    @Column(name = "display_name", length = 200)
    private String displayName;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
