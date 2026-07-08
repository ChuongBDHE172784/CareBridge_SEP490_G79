package com.carebridge.backend.partner.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import com.carebridge.backend.partner.entity.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerProfileRequest {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "Phone is required")
    @VietnamesePhoneNumber
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @URL(message = "Invalid website URL")
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String website;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}
