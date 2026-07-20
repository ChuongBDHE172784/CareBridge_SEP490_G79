package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinCareGroupRequest {
    @NotBlank(message = "Mã mời không được để trống")
    private String code;
}
