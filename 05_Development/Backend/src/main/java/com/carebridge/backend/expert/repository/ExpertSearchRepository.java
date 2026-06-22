package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ExpertSearchRepository extends JpaRepository<ExpertProfile, UUID> {

    @Query("""
        SELECT e FROM ExpertProfile e
        WHERE e.isVerified = true
        AND e.isAvailable = true
        AND (:expertise IS NULL OR :expertise IS EMPTY OR EXISTS (
            SELECT x FROM e.expertiseAreas x WHERE LOWER(x) LIKE LOWER(CONCAT('%', :expertise, '%'))
        ))
        ORDER BY e.avgRating DESC
        """)
    Page<ExpertProfile> searchVerifiedExperts(
        @Param("expertise") String expertise,
        Pageable pageable
    );
}
