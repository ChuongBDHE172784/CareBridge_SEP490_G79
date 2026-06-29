package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Payment transaction repository.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find payment by booking ID.
     *
     * @param bookingId the booking ID
     * @return Optional of payment transaction
     */
    Optional<PaymentTransaction> findByBookingId(Long bookingId);

    /**
     * Find payment by booking reference.
     *
     * @param bookingRef the booking reference
     * @return Optional of payment transaction
     */
    Optional<PaymentTransaction> findByBookingRef(String bookingRef);

    /**
     * Find payments by payer user ID.
     *
     * @param payerUserId the payer user ID
     * @return list of payments
     */
    List<PaymentTransaction> findByPayerUserId(Long payerUserId);

    /**
     * Find payments by status.
     *
     * @param status the payment status
     * @return list of payments
     */
    List<PaymentTransaction> findByStatus(com.carebridge.backend.expert.enums.PaymentStatus status);

    /**
     * Find payment by gateway transaction ID.
     *
     * @param gatewayName the gateway name
     * @param gatewayTransactionId the gateway transaction ID
     * @return Optional of payment
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.gatewayName = :gatewayName " +
            "AND p.gatewayTransactionId = :gatewayTransactionId")
    Optional<PaymentTransaction> findByGateway(@Param("gatewayName") String gatewayName,
                                                @Param("gatewayTransactionId") String gatewayTransactionId);
}
