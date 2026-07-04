package com.carebridge.backend.impactassessment.rating;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "impact_assessment_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactAssessmentRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rating_id", updatable = false, nullable = false)
    private UUID ratingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id")
    private UUID contentId;

    @Column(name = "rating_value", precision = 3, scale = 2)
    private BigDecimal ratingValue;

    @Column(name = "feedback_text", columnDefinition = "text")
    private String feedbackText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
