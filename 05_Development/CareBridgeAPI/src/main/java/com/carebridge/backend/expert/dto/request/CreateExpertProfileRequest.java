package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpertProfileRequest {

    @NotBlank
    @Size(max = 80)
    private String specialtyId;

    @NotBlank
    @Size(max = 36)
    private String hospitalId;

    @Size(max = 100)
    private String specialty;

    @Size(max = 150)
    private String professionalTitle;

    @Min(0)
    @Max(80)
    private Integer experienceYears;

    @Size(max = 200)
    private String workplace;

    @Size(max = 5000)
    private String consultationScope;

    private BigDecimal ratingAvg;

    @PositiveOrZero
    private Long consultationFeeVnd;
}
