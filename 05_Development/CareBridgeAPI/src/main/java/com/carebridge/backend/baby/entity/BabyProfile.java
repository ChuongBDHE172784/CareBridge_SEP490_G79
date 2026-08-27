package com.carebridge.backend.baby.entity;

import com.carebridge.backend.profile.entity.Person;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "care_subjects")
@org.hibernate.annotations.SQLRestriction("subject_type = 'BABY'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BabyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_subject_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 10)
    private Gender gender;

    @Column(name = "birth_weight_kg", precision = 4, scale = 2)
    private BigDecimal birthWeightKg;

    @Column(name = "birth_length_cm", precision = 4, scale = 1)
    private BigDecimal birthLengthCm;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BabyProfileStatus status = BabyProfileStatus.ACTIVE;

    @Transient
    private Boolean active;

    @Builder.Default
    @Column(name = "subject_type", nullable = false, length = 30, updatable = false)
    private String subjectType = "BABY";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Boolean getActive() {
        return active != null ? active : status == BabyProfileStatus.ACTIVE;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @PrePersist
    @PreUpdate
    void canonicalPerson() {
        if (person == null) person = Person.builder().displayName(nickname).dateOfBirth(birthDate).build();
        else {
            person.setDisplayName(nickname);
            person.setDateOfBirth(birthDate);
        }
    }
}
