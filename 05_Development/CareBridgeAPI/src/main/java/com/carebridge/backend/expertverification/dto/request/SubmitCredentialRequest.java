package com.carebridge.backend.expertverification.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SubmitCredentialRequest {

 @NotBlank
 @Pattern(regexp = "MEDICAL_LICENSE|DEGREE|CERTIFICATE|IDENTITY_DOCUMENT|PROFESSIONAL_LICENSE",
          message = "Loai chung chi khong hop le")
 @Size(max = 50)
 private String credentialType;

 @Size(max = 100)
 private String credentialNumber;

 @Size(max = 200)
 private String issuer;

 private String issuedDate;

 private String expiryDate;

 @Size(max = 2000)
 private String reviewNote;

 private MultipartFile file;
}
