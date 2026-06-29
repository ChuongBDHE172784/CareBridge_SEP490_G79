package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Refund record repository.
 */
@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {

    /**
     * Find refunds by payment ID.
     *
     * @param paymentId the payment ID
     * @return list of refund records
     */
    List<RefundRecord> findByPaymentId(Long paymentId);

    /**
     * Find refunds by dispute ID.
     *
     * @param disputeId the dispute ID
     * @return list of refund records
     */
    List<RefundRecord> findByDisputeId(Long disputeId);

    /**
     * Find refunds by status.
     *
     * @param status the refund status
     * @return list of refund records
     */
    List<RefundRecord> findByStatus(String status);

    /**
     * Find refunds approved by a specific admin.
     *
     * @param approvedBy the admin user ID
     * @return list of refund records
     */
    List<RefundRecord> findByApprovedBy(Long approvedBy);
}
