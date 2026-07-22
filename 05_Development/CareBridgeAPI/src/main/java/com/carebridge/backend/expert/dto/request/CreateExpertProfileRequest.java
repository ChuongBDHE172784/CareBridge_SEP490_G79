package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpertProfileRequest {

    @NotBlank
    @Size(max = 5)
    private String specialtyId;

    @NotBlank
    @Size(max = 8)
    private String hospitalId;

    @Size(max = 100)
    private String specialty;

    @Size(max = 150)
    private String professionalTitle;

    private Integer experienceYears;

    @Size(max = 200)
    private String workplace;

    @Size(max = 5000)
    private String consultationScope;

    private BigDecimal ratingAvg;
}
