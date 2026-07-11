package com.carebridge.backend.partner.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record SubmitSponsoredContentRequest(@NotBlank @Size(max=255) String title, String description,
 LocalDate startDate, LocalDate endDate, @NotBlank @Size(max=100) String sponsorLabel) {
 @AssertTrue(message="endDate must not be before startDate")
 public boolean isDateRangeValid(){ return startDate==null||endDate==null||!endDate.isBefore(startDate); }
}
