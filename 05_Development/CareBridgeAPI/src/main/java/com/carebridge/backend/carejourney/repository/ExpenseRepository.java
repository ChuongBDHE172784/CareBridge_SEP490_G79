package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.ownerUserId = :ownerUserId
              AND (:journeyId IS NULL OR e.journeyId = :journeyId)
              AND (:babyId IS NULL OR e.babyId = :babyId)
              AND (:from IS NULL OR e.expenseDate >= :from)
              AND (:to IS NULL OR e.expenseDate <= :to)
            ORDER BY e.expenseDate DESC, e.createdAt DESC
            """)
    List<Expense> findByOwnerFiltered(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // UC53: aggregation raw rows for CATEGORY grouping
    @Query("""
            SELECT e.category AS groupKey, e.currency AS currency,
                   SUM(e.amount) AS totalAmount, COUNT(e) AS cnt
            FROM Expense e
            WHERE e.ownerUserId = :ownerUserId
              AND (:from IS NULL OR e.expenseDate >= :from)
              AND (:to IS NULL OR e.expenseDate <= :to)
            GROUP BY e.category, e.currency
            """)
    List<Object[]> groupByCategory(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // UC53: aggregation raw rows for MONTH grouping
    @Query(value = """
            SELECT TO_CHAR(e.expense_date, 'YYYY-MM') AS group_key,
                   e.currency,
                   SUM(e.amount) AS total_amount,
                   COUNT(*) AS cnt
            FROM expense_entries e
            WHERE e.owner_user_id = :ownerUserId
              AND (:from IS NULL OR e.expense_date >= CAST(:from AS date))
              AND (:to IS NULL OR e.expense_date <= CAST(:to AS date))
            GROUP BY group_key, e.currency
            ORDER BY group_key DESC
            """, nativeQuery = true)
    List<Object[]> groupByMonth(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // UC53: aggregation by stage — requires JOIN to mother_journeys
    @Query(value = """
            SELECT COALESCE(mj.journey_type, 'UNSPECIFIED') AS group_key,
                   e.currency,
                   SUM(e.amount) AS total_amount,
                   COUNT(*) AS cnt
            FROM expense_entries e
            LEFT JOIN mother_journeys mj ON e.mother_journey_id = mj.journey_id
            WHERE e.owner_user_id = :ownerUserId
              AND (:from IS NULL OR e.expense_date >= CAST(:from AS date))
              AND (:to IS NULL OR e.expense_date <= CAST(:to AS date))
            GROUP BY group_key, e.currency
            """, nativeQuery = true)
    List<Object[]> groupByStage(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
