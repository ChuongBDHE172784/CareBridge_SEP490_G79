package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ContributionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContributionPointRepository extends JpaRepository<ContributionPoint, UUID> {

    interface SourceTypeTotal {
        String getSourceType();

        Long getTotalPoints();
    }

    @Query(value = "SELECT COALESCE(SUM((payload->>'points')::int), 0) FROM audit_events WHERE event_category='EXPERT_CONTRIBUTION' AND actor_user_id=:userId", nativeQuery = true)
    int sumPointsByUserId(@Param("userId") UUID userId);


    @Query("SELECT cp FROM ContributionPoint cp WHERE cp.userId = :userId ORDER BY cp.recordedAt DESC")
    List<ContributionPoint> findByUserIdOrderByRecordedAtDesc(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM((payload->>'points')::int), 0) FROM audit_events WHERE event_category='EXPERT_CONTRIBUTION' AND actor_user_id=:userId AND resource_type=:sourceType", nativeQuery = true)
    int sumPointsByUserIdAndSourceType(@Param("userId") UUID userId, @Param("sourceType") String sourceType);

    @Query("""
            SELECT cp.sourceType AS sourceType, SUM(cp.points) AS totalPoints
            FROM ContributionPoint cp
            WHERE cp.userId = :userId
            GROUP BY cp.sourceType
            """)
    List<SourceTypeTotal> sumPointsGroupedBySourceType(@Param("userId") UUID userId);

    // For idempotent point awards - check if record with sourceId exists
    Optional<ContributionPoint> findByUserIdAndSourceTypeAndSourceId(
            @Param("userId") UUID userId,
            @Param("sourceType") String sourceType,
            @Param("sourceId") UUID sourceId);
}
