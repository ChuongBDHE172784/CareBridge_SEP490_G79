package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.carejourney.dto.*;
import com.carebridge.backend.carejourney.entity.Expense;
import com.carebridge.backend.carejourney.repository.ExpenseRepository;
import com.carebridge.backend.carejourney.service.IExpenseService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseServiceImpl implements IExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AuditService auditService;

    @Override
    public ExpenseResponse addExpense(AddExpenseRequest request, UUID userId) {
        // C3: expenseDate must not be in the future (server-side check)
        if (request.expenseDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPENSE-003",
                    "EXPENSE-003: expenseDate cannot be in the future");
        }

        var expense = Expense.builder()
                .ownerUserId(userId)
                .journeyId(request.journeyId())
                .babyId(request.babyId())
                .category(request.category())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "VND")
                .expenseDate(request.expenseDate())
                .note(request.note())
                .build();

        var saved = expenseRepository.save(expense);

        // PDPA ADR-002: DO NOT log amount or note
        auditService.log(AuditAction.EXPENSE_CREATED, userId,
                "Expense", saved.getId().toString(), "created");
        log.info("Expense created: expenseId={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID expenseId, UUID userId) {
        var expense = findOwnedOrThrow(expenseId, userId);
        return toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(UUID userId, ListExpenseFilter filter) {
        return expenseRepository.findByOwnerFiltered(
                        userId,
                        filter.journeyId(),
                        filter.babyId(),
                        filter.from(),
                        filter.to())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request, UUID userId) {
        var expense = findOwnedOrThrow(expenseId, userId);

        // PATCH: apply only non-null fields
        if (request.category() != null) expense.setCategory(request.category());
        if (request.amount() != null)   expense.setAmount(request.amount());
        if (request.expenseDate() != null) {
            if (request.expenseDate().isAfter(LocalDate.now())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPENSE-003",
                        "EXPENSE-003: expenseDate cannot be in the future");
            }
            expense.setExpenseDate(request.expenseDate());
        }
        if (request.note() != null) expense.setNote(request.note());

        var saved = expenseRepository.save(expense);
        auditService.log(AuditAction.EXPENSE_UPDATED, userId,
                "Expense", expenseId.toString(), "updated");
        // PDPA: no amount/note in logs
        log.info("Expense updated: expenseId={}, userId={}", expenseId, userId);
        return toResponse(saved);
    }

    @Override
    public void deleteExpense(UUID expenseId, UUID userId) {
        var expense = findOwnedOrThrow(expenseId, userId);
        // Audit before hard delete (ADR-CJ-052)
        auditService.log(AuditAction.EXPENSE_DELETED, userId,
                "Expense", expenseId.toString(), "deleted");
        expenseRepository.delete(expense);
        log.info("Expense deleted: expenseId={}, userId={}", expenseId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getSummary(UUID userId, String groupBy, LocalDate from, LocalDate to) {
        List<Object[]> rows = switch (groupBy.toUpperCase()) {
            case "MONTH"    -> expenseRepository.groupByMonth(userId, from, to);
            case "CATEGORY" -> expenseRepository.groupByCategory(userId, from, to);
            case "STAGE"    -> expenseRepository.groupByStage(userId, from, to);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPENSE-001",
                    "EXPENSE-001: groupBy must be MONTH, CATEGORY, or STAGE");
        };

        List<ExpenseSummaryBucket> buckets = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        String dominantCurrency = "VND";

        for (Object[] row : rows) {
            String key      = row[0] != null ? row[0].toString() : "UNSPECIFIED";
            String currency = row[1] != null ? row[1].toString() : "VND";
            BigDecimal total = new BigDecimal(row[2].toString());
            long count       = ((Number) row[3]).longValue();
            buckets.add(new ExpenseSummaryBucket(key, total, currency, count));
            // Aggregate grand total only for same currency (mixed-currency guard)
            if (currency.equals(dominantCurrency)) {
                grandTotal = grandTotal.add(total);
            }
        }

        return new ExpenseSummaryResponse(groupBy.toUpperCase(), buckets, grandTotal, dominantCurrency);
    }

    // ── Private ────────────────────────────────────────────────────

    private Expense findOwnedOrThrow(UUID expenseId, UUID userId) {
        return expenseRepository.findByIdAndOwnerUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EXPENSE-004: Expense not found"));
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getOwnerUserId(),
                e.getJourneyId(),
                e.getBabyId(),
                e.getCategory() != null ? e.getCategory().name() : null,
                e.getAmount(),
                e.getCurrency(),
                e.getExpenseDate(),
                e.getNote(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
