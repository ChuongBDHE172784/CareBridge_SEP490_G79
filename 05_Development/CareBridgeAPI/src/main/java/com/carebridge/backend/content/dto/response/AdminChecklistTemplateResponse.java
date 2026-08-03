package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record AdminChecklistTemplateResponse(
        UUID id,
        String name,
        @Nullable @Schema(nullable = true) ContentStage stage,
        ChecklistTemplateType templateType,
        ChecklistTemplateStatus status,
        String description,
        Integer versionNo,
        @Nullable @Schema(nullable = true) Instant updatedAt,
        long itemCount,
        @Nullable @Schema(nullable = true) ReviewFeedbackResponse latestReviewFeedback,
        @Nullable @Schema(nullable = true) Integer displayOrder,
        List<ChecklistRecipientRole> recipientRoles) {

    public AdminChecklistTemplateResponse {
        recipientRoles = recipientRoles == null ? List.of() : List.copyOf(recipientRoles);
    }

    public AdminChecklistTemplateResponse(
            UUID id,
            String name,
            ContentStage stage,
            ChecklistTemplateStatus status,
            String description,
            Integer versionNo,
            Instant updatedAt,
            long itemCount) {
        this(id, name, stage, ChecklistTemplateType.MANDATORY,
                status, description, versionNo, updatedAt, itemCount, null, null, List.of());
    }

    public AdminChecklistTemplateResponse(
            UUID id,
            String name,
            ContentStage stage,
            ChecklistTemplateStatus status,
            String description,
            Integer versionNo,
            Instant updatedAt,
            long itemCount,
            ReviewFeedbackResponse latestReviewFeedback) {
        this(id, name, stage, ChecklistTemplateType.MANDATORY,
                status, description, versionNo, updatedAt, itemCount, latestReviewFeedback, null, List.of());
    }

    /** Source-compatible constructor for the original canonical record shape. */
    public AdminChecklistTemplateResponse(
            UUID id,
            String name,
            ContentStage stage,
            ChecklistTemplateType templateType,
            ChecklistTemplateStatus status,
            String description,
            Integer versionNo,
            Instant updatedAt,
            long itemCount,
            ReviewFeedbackResponse latestReviewFeedback) {
        this(id, name, stage, templateType, status, description, versionNo, updatedAt, itemCount,
                latestReviewFeedback, null, List.of());
    }

    public AdminChecklistTemplateResponse(
            UUID id,
            String name,
            ContentStage stage,
            ChecklistTemplateType templateType,
            ChecklistTemplateStatus status,
            String description,
            Integer versionNo,
            Instant updatedAt,
            long itemCount,
            Integer displayOrder,
            List<ChecklistRecipientRole> recipientRoles,
            ReviewFeedbackResponse latestReviewFeedback) {
        this(id, name, stage, templateType, status, description, versionNo, updatedAt, itemCount,
                latestReviewFeedback, displayOrder, recipientRoles);
    }
}
