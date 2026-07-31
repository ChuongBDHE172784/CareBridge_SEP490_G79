package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
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
    private List<ChecklistItemResponse> items;
}
