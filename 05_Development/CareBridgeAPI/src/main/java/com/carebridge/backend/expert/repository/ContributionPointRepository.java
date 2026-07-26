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

    @Query("SELECT COALESCE(SUM(cp.points), 0) FROM ContributionPoint cp WHERE cp.userId = :userId")
    int sumPointsByUserId(@Param("userId") UUID userId);

    @Query("SELECT cp FROM ContributionPoint cp WHERE cp.userId = :userId ORDER BY cp.recordedAt DESC")
    List<ContributionPoint> findByUserIdOrderByRecordedAtDesc(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(cp.points), 0) FROM ContributionPoint cp WHERE cp.userId = :userId AND cp.sourceType = :sourceType")
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
