package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectExpertRequest {

    @NotBlank
    @Size(max = 2000)
    private String reason;
}
