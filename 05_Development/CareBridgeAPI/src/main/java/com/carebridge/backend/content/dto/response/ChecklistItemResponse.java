package com.carebridge.backend.content.dto.response;

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
public class ChecklistItemResponse {

    private UUID id;
    private String itemText;
    private Integer order;
    private Boolean isRequired;
}
