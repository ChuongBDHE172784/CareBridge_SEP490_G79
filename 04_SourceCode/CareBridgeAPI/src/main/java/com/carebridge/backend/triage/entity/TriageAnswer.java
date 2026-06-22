package com.carebridge.backend.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "triage_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "triage_answer_id")
    private UUID triageAnswerId;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "question_code", length = 80)
    private String questionCode;

    @Column(name = "question_text", length = 500)
    private String questionText;

    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    @Column(name = "answer_order")
    private Integer answerOrder;

    @Column(name = "red_flag_triggered")
    private Boolean redFlagTriggered;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
