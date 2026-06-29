package com.carebridge.backend.payment.repository;

import com.carebridge.backend.payment.entity.ConsultationPriceBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Consultation price band repository.
 * Admin-configured price bands.
 */
@Repository
public interface ConsultationPriceBandRepository extends JpaRepository<ConsultationPriceBand, Long> {

    /**
     * Find active price bands by channel type and duration.
     *
     * @param channelType the channel type
     * @param durationMinutes the duration
     * @return list of active price bands
     */
    @Query("SELECT pb FROM ConsultationPriceBand pb WHERE pb.channelType = :channelType " +
            "AND pb.durationMinutes = :durationMinutes AND pb.status = 'ACTIVE' " +
            "AND (pb.effectiveTo IS NULL OR pb.effectiveTo > NOW()) " +
            "ORDER BY pb.effectiveFrom DESC")
    List<ConsultationPriceBand> findActiveByChannelAndDuration(@Param("channelType") String channelType,
                                                               @Param("durationMinutes") Integer durationMinutes);

    /**
     * Find active price band by channel, duration, and optional specialty.
     *
     * @param channelType the channel type
     * @param durationMinutes the duration
     * @param specialty the specialty (can be null for general)
     * @return Optional of matching price band
     */
    @Query("SELECT pb FROM ConsultationPriceBand pb WHERE pb.channelType = :channelType " +
            "AND pb.durationMinutes = :durationMinutes " +
            "AND (:specialty IS NULL OR pb.specialtyScope = :specialty) " +
            "AND pb.status = 'ACTIVE' " +
            "AND (pb.effectiveTo IS NULL OR pb.effectiveTo > NOW()) " +
            "ORDER BY pb.effectiveFrom DESC")
    Optional<ConsultationPriceBand> findActiveBand(@Param("channelType") String channelType,
                                                    @Param("durationMinutes") Integer durationMinutes,
                                                    @Param("specialty") String specialty);
}
