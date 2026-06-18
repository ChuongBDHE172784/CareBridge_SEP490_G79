package com.carebridge.backend.consent.dto.request;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevokeConsentRequest {

    private Long consentId;
    private ConsentDataType dataType;
    private ConsentPurpose purpose;
}
