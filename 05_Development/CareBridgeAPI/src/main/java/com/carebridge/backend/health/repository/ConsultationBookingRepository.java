package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.ConsultationBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationBookingRepository extends JpaRepository<ConsultationBooking, UUID> {

    @Query("""
            SELECT b FROM ConsultationBooking b
            WHERE b.id = :bookingId
              AND b.requesterUserId = :requesterUserId
              AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
            """)
    Optional<ConsultationBooking> findActiveByIdAndRequester(
            @Param("bookingId") UUID bookingId,
            @Param("requesterUserId") UUID requesterUserId);

    @Modifying
    @Query("""
            UPDATE ConsultationBooking b
            SET b.sharedSummaryId = :summaryId
            WHERE b.id = :bookingId
            """)
    void updateSharedSummaryId(
            @Param("bookingId") UUID bookingId,
            @Param("summaryId") UUID summaryId);

    @Query("""
            SELECT b FROM ConsultationBooking b
            WHERE b.sharedSummaryId = :summaryId
            """)
    List<ConsultationBooking> findBySharedSummaryId(@Param("summaryId") UUID summaryId);
}
