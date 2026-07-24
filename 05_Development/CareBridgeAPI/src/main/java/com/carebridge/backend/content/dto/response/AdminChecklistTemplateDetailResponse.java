package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
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
    private ChecklistTemplateStatus status;
    private String description;
    private List<ChecklistItemResponse> items;
}
