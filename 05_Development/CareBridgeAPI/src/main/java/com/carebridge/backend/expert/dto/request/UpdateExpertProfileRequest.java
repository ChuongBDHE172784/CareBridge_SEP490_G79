package com.carebridge.backend.expert.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpertProfileRequest {

    @Size(max = 80)
    private String specialtyId;

    @Size(max = 20)
    private List<@NotBlank @Size(max = 80) String> specialtyIds;

    @Size(max = 150)
    private String hospitalId;

    @Size(max = 255)
    private String trackAsiaName;

    @Size(max = 500)
    private String trackAsiaAddress;

    private Double trackAsiaLat;
    private Double trackAsiaLng;

    @Size(max = 100)
    private String specialty;

    @Size(max = 150)
    private String professionalTitle;

    @Min(0)
    @Max(80)
    private Integer experienceYears;

    @Size(max = 200)
    private String workplace;

    /** province_id from /api/v1/master-data/provinces; scopes the hospital lookup. */
    @Size(max = 16, message = "Province id must not exceed 16 characters")
    private String workplaceProvinceId;

    @Size(max = 5000)
    private String consultationScope;

    private BigDecimal ratingAvg;

    @PositiveOrZero
    private Long consultationFeeVnd;
}
