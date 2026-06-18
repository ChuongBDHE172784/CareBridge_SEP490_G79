package com.carebridge.backend.security.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String avatarUrl;
}
