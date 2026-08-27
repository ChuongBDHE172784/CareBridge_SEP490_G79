package com.carebridge.backend.carejourney.dto;

import com.carebridge.backend.carejourney.entity.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddExpenseRequest(

        UUID journeyId,

        UUID babyId,

        @NotNull(message = "EXPENSE-001: category is required")
        ExpenseCategory category,

        @NotNull(message = "EXPENSE-002: amount is required")
        @Positive(message = "EXPENSE-002: amount must be positive")
        BigDecimal amount,

        String currency,

        @NotNull(message = "EXPENSE-003: expenseDate is required")
        LocalDate expenseDate,

        String note
) {}
