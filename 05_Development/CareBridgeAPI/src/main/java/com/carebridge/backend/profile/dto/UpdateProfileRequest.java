package com.carebridge.backend.profile.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    @Pattern(regexp = "^[^<>&\"']*$", message = "Display name contains illegal characters")
    String displayName,

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*)?$", message = "Avatar URL must be a valid http/https URL")
    String avatarUrl,

    @Pattern(
        regexp = "^(0[3-9][0-9]{8}|\\+84[3-9][0-9]{8})?$",
        message = "Phone number must be a valid Vietnamese phone number"
    )
    String phoneNumber,

    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth,

    @Size(max = 100, message = "Area must not exceed 100 characters")
    String area
) {}
