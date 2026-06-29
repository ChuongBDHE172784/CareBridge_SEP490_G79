package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.CommissionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Commission record repository.
 */
@Repository
public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {

    /**
     * Find commissions by expert ID.
     *
     * @param expertId the expert ID
     * @return list of commission records
     */
    List<CommissionRecord> findByExpertId(Long expertId);

    /**
     * Find commissions by payment ID.
     *
     * @param paymentId the payment ID
     * @return list of commission records
     */
    List<CommissionRecord> findByPaymentId(Long paymentId);

    /**
     * Find commissions by settlement status.
     *
     * @param status the settlement status
     * @return list of commissions
     */
    List<CommissionRecord> findBySettlementStatus(String status);

    /**
     * Find eligible commissions (for settlement).
     *
     * @param status the status filter (usually PENDING)
     * @param now current time for eligibility check
     * @return list of eligible commissions
     */
    @Query("SELECT c FROM CommissionRecord c WHERE c.settlementStatus = :status " +
            "AND c.eligibleAt IS NOT NULL AND c.eligibleAt <= :now")
    List<CommissionRecord> findEligible(@Param("status") String status, @Param("now") java.time.Instant now);

    /**
     * Calculate total pending commission for an expert.
     *
     * @param expertId the expert ID
     * @return sum of expert net amounts
     */
    @Query("SELECT SUM(c.expertNetAmount) FROM CommissionRecord c " +
            "WHERE c.expertId = :expertId AND c.settlementStatus = 'PENDING'")
    Integer sumPendingByExpertId(@Param("expertId") Long expertId);
}
