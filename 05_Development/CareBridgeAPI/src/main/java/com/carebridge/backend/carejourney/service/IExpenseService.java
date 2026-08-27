package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IExpenseService {

    ExpenseResponse addExpense(AddExpenseRequest request, UUID userId);

    ExpenseResponse getExpense(UUID expenseId, UUID userId);

    List<ExpenseResponse> listExpenses(UUID userId, ListExpenseFilter filter);

    ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID userId);

    void deleteExpense(UUID expenseId, UUID userId);

    ExpenseSummaryResponse getSummary(UUID userId, String groupBy, LocalDate from, LocalDate to);
}
