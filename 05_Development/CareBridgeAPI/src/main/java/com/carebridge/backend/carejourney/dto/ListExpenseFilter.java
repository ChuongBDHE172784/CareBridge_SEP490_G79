package com.carebridge.backend.carejourney.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ListExpenseFilter(
        UUID journeyId,
        UUID babyId,
        LocalDate from,
        LocalDate to
) {}
