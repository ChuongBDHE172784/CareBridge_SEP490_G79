package com.carebridge.backend.safety.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorSelfTestCompletionRequest {

    @NotBlank
    @Pattern(regexp = "TIMEOUT|NEED_HELP")
    private String outcome;
}
