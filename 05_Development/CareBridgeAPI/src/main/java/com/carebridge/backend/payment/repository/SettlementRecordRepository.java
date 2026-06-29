package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Settlement record repository.
 */
@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {

    /**
     * Find settlements by expert ID.
     *
     * @param expertId the expert ID
     * @return list of settlements
     */
    List<SettlementRecord> findByExpertId(Long expertId);

    /**
     * Find settlement by commission ID.
     *
     * @param commissionId the commission ID
     * @return Optional of settlement
     */
    Optional<SettlementRecord> findByCommissionId(Long commissionId);

    /**
     * Find settlements by status.
     *
     * @param status the settlement status
     * @return list of settlements
     */
    List<SettlementRecord> findByStatus(com.carebridge.backend.expert.enums.SettlementStatus status);

    /**
     * Find settlements in a date range.
     *
     * @param from start date
     * @param to end date
     * @return list of settlements
     */
    @Query("SELECT s FROM SettlementRecord s WHERE s.settlementPeriodStart >= :from AND s.settlementPeriodEnd <= :to")
    List<SettlementRecord> findBySettlementPeriodBetween(@Param("from") LocalDate from,
                                                          @Param("to") LocalDate to);

    /**
     * Calculate total settled amount for an expert in a period.
     *
     * @param expertId the expert ID
     * @param from start date
     * @param to end date
     * @return total net amount
     */
    @Query("SELECT SUM(s.expertNetAmount) FROM SettlementRecord s " +
            "WHERE s.expertId = :expertId AND s.status = 'COMPLETED' " +
            "AND s.settlementPeriodStart >= :from AND s.settlementPeriodEnd <= :to")
    Integer sumCompletedByExpertIdAndPeriod(@Param("expertId") Long expertId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);
}
