package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, UUID> {

    boolean existsByTopicId(UUID topicId);

    // ADR-COM-006: used to gate answer posting — only APPROVED questions accept answers
    Optional<CommunityQuestion> findByIdAndStatus(UUID id, QuestionStatus status);

    // ADR-COM-015: batch count of APPROVED questions per topic (for CommunityTopicResponse.questionCount),
    // avoids N+1 — one query for the whole topic list, same pattern as the existing follow-state hydration.
    @Query("""
            SELECT q.topicId AS topicId, COUNT(q) AS cnt
            FROM CommunityQuestion q
            WHERE (q.status = com.carebridge.backend.community.entity.QuestionStatus.APPROVED
                OR q.status = com.carebridge.backend.community.entity.QuestionStatus.LOCKED)
              AND q.topicId IN :topicIds
            GROUP BY q.topicId
            """)
    List<TopicQuestionCountProjection> countApprovedQuestionsByTopicIds(@Param("topicIds") List<UUID> topicIds);

    Page<CommunityQuestion> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<CommunityQuestion> findAllByAuthorIdAndStatusNotOrderByCreatedAtDesc(
            UUID authorId, QuestionStatus status, Pageable pageable);

    // CB-MOD-IMP-004 (Pending Content Queue, ADR-006): list PENDING questions directly,
    // independent of ContentReport — for first-time moderation discovery
    Page<CommunityQuestion> findByStatus(QuestionStatus status, Pageable pageable);

    @Query("""
            SELECT q FROM CommunityQuestion q
            WHERE q.status = :status
              AND NOT EXISTS (
                    SELECT r.id FROM ContentReport r
                    WHERE r.targetId = q.id
                      AND r.targetType = :targetType
                      AND r.status IN :openStatuses
              )
            """)
    Page<CommunityQuestion> findByStatusWithoutOpenModerationCase(
            @Param("status") QuestionStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("openStatuses") Collection<ReportStatus> openStatuses,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM CommunityQuestion q WHERE q.id = :questionId")
    Optional<CommunityQuestion> findByIdForModerationUpdate(@Param("questionId") UUID questionId);

    // UC-198: the shared community feed shows APPROVED and LOCKED questions for all viewers.
    @Query("""
            SELECT q FROM CommunityQuestion q
            WHERE (q.status = com.carebridge.backend.community.entity.QuestionStatus.APPROVED
                OR q.status = com.carebridge.backend.community.entity.QuestionStatus.LOCKED)
              AND (:topicId IS NULL OR q.topicId = :topicId)
            ORDER BY q.createdAt DESC
            """)
    Page<CommunityQuestion> findFeedVisible(
            @Param("topicId") UUID topicId,
            Pageable pageable);

    // UC-162: search — APPROVED or LOCKED, multi-filter, parameterized (ADR-COM-007, OWASP A03)
    @Query("""
            SELECT q FROM CommunityQuestion q
            WHERE (q.status = com.carebridge.backend.community.entity.QuestionStatus.APPROVED
                OR q.status = com.carebridge.backend.community.entity.QuestionStatus.LOCKED)
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(q.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(q.body) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
              AND (:topicId IS NULL OR q.topicId = :topicId)
              AND (:stage IS NULL OR q.stage = :stage)
              AND (:urgency IS NULL OR q.urgency = :urgency)
              AND (:hasExpertAnswer IS NULL
                   OR (:hasExpertAnswer = true AND q.answerCount > 0 AND EXISTS (
                       SELECT a FROM CommunityAnswer a
                       WHERE a.questionId = q.id AND a.expertLabeled = true))
                   OR (:hasExpertAnswer = false AND NOT EXISTS (
                       SELECT a FROM CommunityAnswer a
                       WHERE a.questionId = q.id AND a.expertLabeled = true)))
            ORDER BY q.createdAt DESC
            """)
    Page<CommunityQuestion> searchApproved(
            @Param("keyword") String keyword,
            @Param("topicId") UUID topicId,
            @Param("stage") PregnancyStage stage,
            @Param("urgency") UrgencyLevel urgency,
            @Param("hasExpertAnswer") Boolean hasExpertAnswer,
            Pageable pageable);

    // UC-201: decrement answer_count when an APPROVED answer is soft-deleted; never goes below 0
    @Modifying
    @Query("UPDATE CommunityQuestion q SET q.answerCount = q.answerCount - 1 WHERE q.id = :questionId AND q.answerCount > 0")
    void decrementAnswerCount(@Param("questionId") UUID questionId);

    // Moderation approve: an answer entering APPROVED status becomes visible in the count
    @Modifying
    @Query("UPDATE CommunityQuestion q SET q.answerCount = q.answerCount + 1 WHERE q.id = :questionId")
    void incrementAnswerCount(@Param("questionId") UUID questionId);

    // Atomic moderation transition: prevents concurrent moderators from recording duplicate LOCK actions.
    @Modifying
    @Query(value = "UPDATE public.community_content "
            + "SET moderation_status = 'LOCKED', updated_at = CURRENT_TIMESTAMP "
            + "WHERE content_id = :questionId AND content_type = 'QUESTION' "
            + "AND moderation_status = 'APPROVED'", nativeQuery = true)
    int lockIfApproved(@Param("questionId") UUID questionId);

    // UC-111: dashboard aggregation — question count grouped by status
    @Query("SELECT q.status, COUNT(q) FROM CommunityQuestion q GROUP BY q.status")
    List<Object[]> countGroupByStatus();

    // UC-111: dashboard aggregation — new questions within the reporting window
    long countByCreatedAtBetween(Instant from, Instant to);

    // UC-111: dashboard aggregation — trending topics, excludes hidden topics (TDS §5.2/ADR-004 C6)
    @Query("""
            SELECT q.topicId, t.name, COUNT(q)
            FROM CommunityQuestion q JOIN CommunityTopic t ON t.id = q.topicId
            WHERE t.isHidden = false AND q.createdAt BETWEEN :from AND :to
            GROUP BY q.topicId, t.name
            ORDER BY COUNT(q) DESC
            """)
    List<Object[]> findTrendingTopics(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
