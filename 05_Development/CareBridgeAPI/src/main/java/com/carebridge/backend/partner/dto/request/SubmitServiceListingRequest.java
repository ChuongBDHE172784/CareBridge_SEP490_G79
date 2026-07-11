package com.carebridge.backend.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.URL;

public record SubmitServiceListingRequest(@NotBlank @Size(max = 200) String serviceName,
        String description, @PositiveOrZero BigDecimal priceFrom, @Size(max = 10) String currency,
        @URL String bookingUrl) {}
