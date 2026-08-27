package com.carebridge.backend.carejourney.controller;

import com.carebridge.backend.carejourney.dto.*;
import com.carebridge.backend.carejourney.service.IExpenseService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final IExpenseService expenseService;

    // UC51: Add expense
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> addExpense(
            @Valid @RequestBody AddExpenseRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = expenseService.addExpense(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Expense added"));
    }

    // UC51: Get expense by ID
    @GetMapping("/{expenseId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable UUID expenseId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = expenseService.getExpense(expenseId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC51: List expenses
    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> listExpenses(
            @Valid ListExpenseFilter filter,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = expenseService.listExpenses(callerId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC52: Partial update expense
    @PatchMapping("/{expenseId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = expenseService.updateExpense(expenseId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense updated"));
    }

    // UC52: Delete expense (hard delete)
    @DeleteMapping("/{expenseId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable UUID expenseId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        expenseService.deleteExpense(expenseId, callerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted"));
    }

    // UC53: View expense summary (aggregated, no PII)
    @GetMapping("/summary")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getSummary(
            @RequestParam(defaultValue = "MONTH") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = expenseService.getSummary(callerId, groupBy, from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
