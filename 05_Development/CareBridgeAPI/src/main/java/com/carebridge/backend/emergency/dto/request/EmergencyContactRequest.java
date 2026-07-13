package com.carebridge.backend.emergency.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 32)
    private String phone;

    @Size(max = 80)
    private String relationship;

    private boolean primaryContact = true;
}
