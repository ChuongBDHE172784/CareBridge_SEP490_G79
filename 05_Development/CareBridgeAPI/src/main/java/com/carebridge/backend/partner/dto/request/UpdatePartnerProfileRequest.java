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
public class UpdatePartnerProfileRequest {
    @NotBlank @Size(min = 2, max = 200) private String name;
    @NotNull private OrganizationType type;
    @NotBlank @Size(max = 500) private String address;
    @NotBlank @Size(max = 100) private String city;
    @NotBlank @VietnamesePhoneNumber private String phone;
    @NotBlank @Email private String email;
    @URL @Size(max = 500) private String website;
    @Size(max = 1000) private String logoUrl;
    @Size(max = 2000) private String description;
}
