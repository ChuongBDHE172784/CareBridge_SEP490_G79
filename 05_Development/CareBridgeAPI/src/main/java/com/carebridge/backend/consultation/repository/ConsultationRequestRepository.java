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

    /**
     * Buoi tu van gan voi cuoc tro chuyen nay. Chat mo ra tu mot yeu cau da nhan, va
     * dong lai khi khung gio cua yeu cau do troi qua — khong co no thi moi cuoc tro
     * chuyen mo mot lan roi mo mai mai.
     */
    Optional<ConsultationRequest> findFirstByDirectConversationIdAndStatusOrderByCreatedAtDesc(
            UUID directConversationId, ConsultationRequestStatus status);

    /** The mother's live request, if she has one — PENDING or already accepted. */
    Optional<ConsultationRequest> findFirstByRequesterUserIdAndStatusIn(
            UUID requesterUserId, java.util.Collection<ConsultationRequestStatus> statuses);

    /**
     * Has anyone already taken this hour from this expert? The slot row stays
     * AVAILABLE until the expert acts on the request, so the claim lives here.
     */
    boolean existsByExpertProfileIdAndPreferredWindowStartAndStatusIn(
            UUID expertProfileId, Instant preferredWindowStart,
            java.util.Collection<ConsultationRequestStatus> statuses);

    Page<ConsultationRequest> findByExpertProfileIdAndStatus(
            UUID expertProfileId, ConsultationRequestStatus status, Pageable pageable);

    Page<ConsultationRequest> findByExpertProfileId(UUID expertProfileId, Pageable pageable);

    long countByExpertProfileIdAndStatus(
            UUID expertProfileId, ConsultationRequestStatus status);

    /**
     * Đếm yêu cầu đang chờ theo từng chuyên gia trong một lần truy vấn — hàm vét dùng để cân tải
     * ứng viên "yêu cầu mở". Gọi {@link #countByExpertProfileIdAndStatus} trong vòng lặp sẽ thành
     * N+1 ngay trên đường điều phối.
     */
    @Query("select r.expertProfileId, count(r) from ConsultationRequest r"
            + " where r.expertProfileId in :expertProfileIds"
            + " and r.status = com.carebridge.backend.consultation.entity.ConsultationRequestStatus.PENDING"
            + " group by r.expertProfileId")
    List<Object[]> countPendingByExpert(
            @Param("expertProfileIds") java.util.Collection<UUID> expertProfileIds);

    // Canonical model: the expert profile IS the users row, so the profile id is the user id.
    @Query(
            value = "select user_id from users where user_id = :expertProfileId and role = 'EXPERT'",
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
