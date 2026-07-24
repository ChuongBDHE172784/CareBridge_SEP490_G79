package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, UUID> {

    Optional<ConsultationRequest> findByRequesterUserIdAndClientRequestId(
            UUID requesterUserId, UUID clientRequestId);

    Page<ConsultationRequest> findByRequesterUserIdAndStatus(
            UUID requesterUserId, ConsultationRequestStatus status, Pageable pageable);

    Page<ConsultationRequest> findByRequesterUserId(UUID requesterUserId, Pageable pageable);

    Page<ConsultationRequest> findByExpertProfileIdAndStatus(
            UUID expertProfileId, ConsultationRequestStatus status, Pageable pageable);

    Page<ConsultationRequest> findByExpertProfileId(UUID expertProfileId, Pageable pageable);

    long countByExpertProfileIdAndStatus(
            UUID expertProfileId, ConsultationRequestStatus status);

    @Query(
            value = "select user_id from professional_profiles where professional_profile_id = :expertProfileId",
            nativeQuery = true)
    Optional<UUID> findAssignedExpertUserId(
            @Param("expertProfileId") UUID expertProfileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConsultationRequest r
               set r.status = :to,
                   r.respondedAt = :respondedAt,
                   r.respondedBy = :respondedBy,
                   r.rejectReason = :rejectReason,
                   r.directConversationId = :directConversationId,
                   r.updatedAt = :respondedAt
             where r.id = :id
               and r.status = com.carebridge.backend.consultation.entity.ConsultationRequestStatus.PENDING
            """)
    int tryTransition(
            @Param("id") UUID id,
            @Param("to") ConsultationRequestStatus to,
            @Param("respondedAt") Instant respondedAt,
            @Param("respondedBy") UUID respondedBy,
            @Param("rejectReason") String rejectReason,
            @Param("directConversationId") UUID directConversationId);

    @Query("""
            select r.id from ConsultationRequest r
             where r.status = com.carebridge.backend.consultation.entity.ConsultationRequestStatus.PENDING
               and r.expiresAt < :now
             order by r.expiresAt asc
            """)
    List<UUID> findExpiredIds(@Param("now") Instant now, Pageable pageable);
}
