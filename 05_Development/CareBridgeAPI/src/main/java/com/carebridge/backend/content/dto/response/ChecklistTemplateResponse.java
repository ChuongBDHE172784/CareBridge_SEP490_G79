package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateResponse {

    private UUID id;
    private String name;
    private ContentStage stage;
    private String description;
    private ChecklistTemplateType templateType;
    private Short checklistContractVersion;
    /** Optional cadence projection; absent on legacy roots. */
    private Integer planNumber;
    private String section;
    private ChecklistScheduleType scheduleType;
    private ChecklistMaterializationPolicy materializationPolicy;
    private String scheduleGroupKey;
    private ChecklistCareContextType scheduleContextType;
    private ChecklistScheduleEndMode scheduleEndMode;
    private ChecklistWeekBoundaryRule weekBoundaryRule;
    private Integer eligibilityStartInclusive;
    private Integer eligibilityEndInclusive;
    private List<ChecklistItemResponse> items;
}
