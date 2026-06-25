package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.ExpertConsultationPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Expert consultation price repository.
 * Manages expert-specific pricing with versioning.
 */
@Repository
public interface ExpertConsultationPriceRepository extends JpaRepository<ExpertConsultationPrice, Long> {

    /**
     * Find active price for expert by channel and duration.
     *
     * @param expertId the expert ID
     * @param channelType the channel type
     * @param durationMinutes the duration
     * @return Optional of active price
     */
    @Query("SELECT p FROM ExpertConsultationPrice p WHERE p.expertId = :expertId " +
            "AND p.channelType = :channelType AND p.durationMinutes = :durationMinutes " +
            "AND p.status = 'ACTIVE' " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo > NOW()) " +
            "ORDER BY p.effectiveFrom DESC")
    Optional<ExpertConsultationPrice> findActivePrice(@Param("expertId") Long expertId,
                                                       @Param("channelType") String channelType,
                                                       @Param("durationMinutes") Integer durationMinutes);

    /**
     * Find all prices for an expert.
     *
     * @param expertId the expert ID
     * @return list of prices
     */
    List<ExpertConsultationPrice> findByExpertId(Long expertId);

    /**
     * Find prices that will expire at or before a given date.
     * Useful for scheduled job to expire old prices.
     *
     * @param beforeDate the date
     * @return list of expiring prices
     */
    @Query("SELECT p FROM ExpertConsultationPrice p WHERE p.status = 'ACTIVE' " +
            "AND p.effectiveTo IS NOT NULL AND p.effectiveTo <= :beforeDate")
    List<ExpertConsultationPrice> findExpiringBefore(@Param("beforeDate") Instant beforeDate);
}
