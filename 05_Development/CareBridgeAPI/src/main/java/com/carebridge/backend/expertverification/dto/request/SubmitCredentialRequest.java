package com.carebridge.backend.expertverification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitCredentialRequest {

    @NotBlank(message = "credentialType must not be blank")
    @Size(max = 50)
    private String credentialType;

    @Size(max = 100)
    private String credentialNumber;

    @Size(max = 200)
    private String issuer;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    @Size(max = 500)
    private String fileUrl;

    @Size(max = 2000)
    private String reviewNote;
}
