package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpertReviewRepository extends JpaRepository<ExpertReview, UUID> {

    List<ExpertReview> findByExpertId(UUID expertId);

    List<ExpertReview> findByMotherId(UUID motherId);

    boolean existsByExpertIdAndMotherIdAndBookingId(UUID expertId, UUID motherId, UUID bookingId);
}
