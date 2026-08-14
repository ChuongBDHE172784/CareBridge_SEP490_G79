package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistSupportFunction;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChecklistItemResponse {

    private UUID id;
    private String itemText;
    private String description;
    private Integer order;
    private Boolean isRequired;
    private ChecklistTargetSubject targetSubject;
    private ChecklistSupportFunction supportFunction;
    private Boolean repeatWeekly;
    private Boolean repeatDaily;

    /** Compatibility constructor for the pre-detail response shape. */
    public ChecklistItemResponse(
            UUID id,
            String itemText,
            Integer order,
            Boolean isRequired,
            ChecklistTargetSubject targetSubject) {
        this(id, itemText, null, order, isRequired, targetSubject, null, false, false);
    }
}
