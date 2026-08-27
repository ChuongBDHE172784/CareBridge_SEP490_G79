package com.carebridge.backend.carejourney.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID expenseId,
        UUID ownerUserId,
        UUID journeyId,
        UUID babyId,
        String category,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String note,
        Instant createdAt,
        Instant updatedAt
) {}
