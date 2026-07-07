package com.carebridge.backend.carejourney;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.carejourney.dto.AddExpenseRequest;
import com.carebridge.backend.carejourney.dto.ListExpenseFilter;
import com.carebridge.backend.carejourney.dto.UpdateExpenseRequest;
import com.carebridge.backend.carejourney.entity.Expense;
import com.carebridge.backend.carejourney.entity.ExpenseCategory;
import com.carebridge.backend.carejourney.repository.ExpenseRepository;
import com.carebridge.backend.carejourney.service.impl.ExpenseServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    // Props Isolation — synthetic IDs (PDPA: never real amount/note in IDs)
    static final UUID OWNER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000051");
    static final UUID OTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000052");
    static final UUID EXPENSE_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

    @Mock private ExpenseRepository expenseRepository;
    @Mock private AuditService auditService;
    @InjectMocks private ExpenseServiceImpl service;

    private Expense makeSavedExpense() {
        return Expense.builder()
                .id(EXPENSE_ID)
                .ownerUserId(OWNER_ID)
                .category(ExpenseCategory.CHECKUP)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .expenseDate(LocalDate.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private AddExpenseRequest makeValidRequest() {
        return new AddExpenseRequest(
                null, null, ExpenseCategory.CHECKUP,
                new BigDecimal("150000"), "VND", LocalDate.now(), null
        );
    }

    // EXPENSE-TC-001: addExpense — valid → returns ExpenseResponse
    @Test
    void addExpense_valid_returnsResponse() {
        when(expenseRepository.save(any())).thenReturn(makeSavedExpense());

        var response = service.addExpense(makeValidRequest(), OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.expenseId()).isEqualTo(EXPENSE_ID);
        assertThat(response.category()).isEqualTo("CHECKUP");
    }

    // EXPENSE-TC-002: addExpense — future date → EXPENSE-003 / BusinessException
    @Test
    void addExpense_futureDate_throwsBusinessException() {
        var request = new AddExpenseRequest(
                null, null, ExpenseCategory.CHECKUP,
                new BigDecimal("150000"), "VND", LocalDate.now().plusDays(1), null
        );

        assertThatThrownBy(() -> service.addExpense(request, OWNER_ID))
                .isInstanceOf(BusinessException.class);
    }

    // EXPENSE-TC-003: getExpense — owner reads own → returns ExpenseResponse
    @Test
    void getExpense_owner_returnsResponse() {
        when(expenseRepository.findByIdAndOwnerUserId(EXPENSE_ID, OWNER_ID))
                .thenReturn(Optional.of(makeSavedExpense()));

        var response = service.getExpense(EXPENSE_ID, OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.expenseId()).isEqualTo(EXPENSE_ID);
    }

    // EXPENSE-TC-004: getExpense — not found → ResourceNotFoundException (EXPENSE-004)
    @Test
    void getExpense_notFound_throwsResourceNotFound() {
        when(expenseRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExpense(UUID.randomUUID(), OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // EXPENSE-TC-005: getExpense — non-owner → ResourceNotFoundException (EXPENSE-004 / ADR-CJ-052-01)
    @Test
    void getExpense_nonOwner_throwsResourceNotFound() {
        when(expenseRepository.findByIdAndOwnerUserId(EXPENSE_ID, OTHER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExpense(EXPENSE_ID, OTHER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // EXPENSE-TC-006: listExpenses — returns owner's list
    @Test
    void listExpenses_returnsOwnerList() {
        when(expenseRepository.findByOwnerFiltered(any(), any(), any(), any(), any()))
                .thenReturn(List.of(makeSavedExpense()));

        var filter = new ListExpenseFilter(null, null, null, null);
        var result = service.listExpenses(OWNER_ID, filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).expenseId()).isEqualTo(EXPENSE_ID);
    }

    // EXPENSE-TC-007: listExpenses — empty → empty list
    @Test
    void listExpenses_empty_returnsEmptyList() {
        when(expenseRepository.findByOwnerFiltered(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var filter = new ListExpenseFilter(null, null, null, null);
        var result = service.listExpenses(OWNER_ID, filter);

        assertThat(result).isEmpty();
    }

    // EXPENSE-TC-008: updateExpense — partial update → returns updated response (UC52)
    @Test
    void updateExpense_partialUpdate_returnsUpdated() {
        var updated = Expense.builder()
                .id(EXPENSE_ID).ownerUserId(OWNER_ID)
                .category(ExpenseCategory.DELIVERY)
                .amount(new BigDecimal("200000")).currency("VND")
                .expenseDate(LocalDate.now())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(expenseRepository.findByIdAndOwnerUserId(EXPENSE_ID, OWNER_ID))
                .thenReturn(Optional.of(makeSavedExpense()));
        when(expenseRepository.save(any())).thenReturn(updated);

        var request = new UpdateExpenseRequest(ExpenseCategory.DELIVERY, new BigDecimal("200000"), null, null);
        var response = service.updateExpense(EXPENSE_ID, request, OWNER_ID);

        assertThat(response.category()).isEqualTo("DELIVERY");
    }

    // EXPENSE-TC-009: deleteExpense — hard delete → no exception (UC52)
    @Test
    void deleteExpense_owner_deletesSuccessfully() {
        when(expenseRepository.findByIdAndOwnerUserId(EXPENSE_ID, OWNER_ID))
                .thenReturn(Optional.of(makeSavedExpense()));
        doNothing().when(expenseRepository).delete(any());

        service.deleteExpense(EXPENSE_ID, OWNER_ID);
        // no exception → pass
    }

    // EXPENSE-TC-010: getSummary — MONTH grouping → returns ExpenseSummaryResponse (UC53)
    @Test
    void getSummary_monthGrouping_returnsResponse() {
        when(expenseRepository.groupByMonth(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var response = service.getSummary(OWNER_ID, "MONTH", null, null);

        assertThat(response).isNotNull();
        assertThat(response.groupBy()).isEqualTo("MONTH");
    }

    // EXPENSE-TC-011: getSummary — CATEGORY grouping → returns response (UC53)
    @Test
    void getSummary_categoryGrouping_returnsResponse() {
        when(expenseRepository.groupByCategory(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var response = service.getSummary(OWNER_ID, "CATEGORY", null, null);

        assertThat(response.groupBy()).isEqualTo("CATEGORY");
    }

    // EXPENSE-TC-012: getSummary — STAGE grouping → returns response (UC53)
    @Test
    void getSummary_stageGrouping_returnsResponse() {
        when(expenseRepository.groupByStage(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var response = service.getSummary(OWNER_ID, "STAGE", null, null);

        assertThat(response.groupBy()).isEqualTo("STAGE");
    }

    // EXPENSE-TC-013: getSummary — invalid groupBy → BusinessException (UC53)
    @Test
    void getSummary_invalidGroupBy_throwsBusinessException() {
        assertThatThrownBy(() -> service.getSummary(OWNER_ID, "INVALID", null, null))
                .isInstanceOf(BusinessException.class);
    }
}
