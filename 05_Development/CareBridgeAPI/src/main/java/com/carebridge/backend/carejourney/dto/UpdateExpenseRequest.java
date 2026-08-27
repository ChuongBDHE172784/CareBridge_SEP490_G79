package com.carebridge.backend.carejourney.dto;

import com.carebridge.backend.carejourney.entity.ExpenseCategory;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseRequest(

        ExpenseCategory category,

        @Positive(message = "EXPENSE-002: amount must be positive")
        BigDecimal amount,

        LocalDate expenseDate,

        String note
) {}
