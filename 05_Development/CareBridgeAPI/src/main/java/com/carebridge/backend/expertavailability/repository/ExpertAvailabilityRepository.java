package com.carebridge.backend.expertavailability.repository;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, UUID> {
    List<ExpertAvailability> findByExpertProfileId(UUID expertProfileId);
    List<ExpertAvailability> findByExpertProfileIdAndStartAtGreaterThanEqualAndStartAtLessThan(
            UUID expertProfileId, Instant startAt, Instant endAt);
    List<ExpertAvailability> findByExpertProfileIdAndEndAtAfterOrderByStartAtAsc(
            UUID expertProfileId, Instant after);
    boolean existsByExpertProfileIdAndStartAtAndEndAtAndStatus(
            UUID expertProfileId,
            Instant startAt,
            Instant endAt,
            AvailabilityStatus status);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM consultation_bookings WHERE availability_id = :availabilityId)", nativeQuery = true)
    boolean isReferencedByBooking(@Param("availabilityId") UUID availabilityId);
    Optional<ExpertAvailability> findTopByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);
}
