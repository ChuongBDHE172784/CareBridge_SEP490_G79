package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotBlank(message = "status must not be blank")
    private String status;
}
