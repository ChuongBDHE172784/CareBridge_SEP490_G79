package com.carebridge.backend.consent.dto.request;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrantConsentRequest {

    @NotNull
    private ConsentDataType dataType;

    @NotNull
    private ConsentPurpose purpose;

    @Size(max = 120)
    private String recipient;

    @Size(max = 1000)
    private String scope;

    @Positive
    private Integer expiryDays;
}
