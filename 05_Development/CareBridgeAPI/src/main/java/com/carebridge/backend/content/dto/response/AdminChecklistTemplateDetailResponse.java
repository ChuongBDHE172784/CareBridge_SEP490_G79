package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Admin-only checklist detail. Consumer checklist responses deliberately omit review status. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChecklistTemplateDetailResponse {

    private UUID id;
    private String name;
    private ContentStage stage;
    private Integer displayOrder;
    private ChecklistTemplateStatus status;
    private String description;
    private Integer versionNo;
    private UUID lineageId;
    private UUID versionId;
    private List<ChecklistRecipientRole> recipientRoles;
    private ChecklistSubstageResponse substage;
    private Boolean migrationReviewRequired;
    private Boolean distributionEnabled;
    private ChecklistTemplateType templateType;
    private Instant approvedAt;
    private UUID approvedBy;
    private Instant migrationReviewedAt;
    private UUID migrationReviewedBy;
    private List<ChecklistItemResponse> items;
    private ReviewFeedbackResponse latestReviewFeedback;
}
