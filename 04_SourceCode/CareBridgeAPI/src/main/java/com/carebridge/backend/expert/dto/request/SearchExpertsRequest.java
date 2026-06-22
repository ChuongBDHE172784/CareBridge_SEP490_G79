package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchExpertsRequest {

    @Size(max = 100, message = "Expertise area must not exceed 100 characters")
    private String expertise;

    private Integer page;

    private Integer size;
}
