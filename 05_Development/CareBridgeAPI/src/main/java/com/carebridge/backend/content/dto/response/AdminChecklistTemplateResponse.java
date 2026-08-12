package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
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
        List<ChecklistRecipientRole> recipientRoles,
        @Nullable @Schema(nullable = true) Short checklistContractVersion,
        @Nullable @Schema(nullable = true) Integer planNumber,
        @Nullable @Schema(nullable = true) String section,
        @Nullable @Schema(nullable = true) ChecklistScheduleType scheduleType,
        @Nullable @Schema(nullable = true) ChecklistMaterializationPolicy materializationPolicy,
        @Nullable @Schema(nullable = true) String scheduleGroupKey,
        @Nullable @Schema(nullable = true) ChecklistCareContextType scheduleContextType,
        @Nullable @Schema(nullable = true) ChecklistScheduleEndMode scheduleEndMode,
        @Nullable @Schema(nullable = true) ChecklistWeekBoundaryRule weekBoundaryRule,
        @Nullable @Schema(nullable = true) Integer eligibilityStartInclusive,
        @Nullable @Schema(nullable = true) Integer eligibilityEndInclusive,
        @Nullable @Schema(nullable = true) String checklistQuarantineReasonCode,
        @Nullable @Schema(nullable = true) String provenanceStatus) {

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
                status, description, versionNo, updatedAt, itemCount, null, null, List.of(), null,
                null, null, null, null, null, null, null, null, null, null, null, null);
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
                status, description, versionNo, updatedAt, itemCount, latestReviewFeedback, null, List.of(), null,
                null, null, null, null, null, null, null, null, null, null, null, null);
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
                latestReviewFeedback, null, List.of(), null,
                null, null, null, null, null, null, null, null, null, null, null, null);
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
                latestReviewFeedback, displayOrder, recipientRoles, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
