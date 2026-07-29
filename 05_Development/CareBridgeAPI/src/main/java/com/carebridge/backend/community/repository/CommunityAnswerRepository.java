package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, UUID> {

    // UC-198: batch-check which questions in a set have at least one expert-labeled APPROVED answer
    @Query("SELECT DISTINCT a.questionId FROM CommunityAnswer a WHERE a.questionId IN :questionIds AND a.expertLabeled = true AND a.status = 'APPROVED'")
    Set<UUID> findQuestionIdsWithExpertAnswer(java.util.Collection<UUID> questionIds);

    // Dev seed idempotency (DevDataSeeder) — tolerate legacy duplicate answers
    // so development startup is not blocked by existing sample data.
    Optional<CommunityAnswer> findFirstByQuestionIdAndAuthorIdOrderByCreatedAtAsc(UUID questionId, UUID authorId);

    // CB-MOD-IMP-004 (Pending Content Queue, ADR-006): list PENDING answers directly,
    // independent of ContentReport — for first-time moderation discovery
    Page<CommunityAnswer> findByStatus(AnswerStatus status, Pageable pageable);

    // The answer is public only when both the answer and its parent question are APPROVED.
    @Query("""
            SELECT a FROM CommunityAnswer a
            WHERE a.status = com.carebridge.backend.community.entity.AnswerStatus.APPROVED
              AND EXISTS (
                    SELECT q.id FROM CommunityQuestion q
                    WHERE q.id = a.questionId
                      AND q.status = com.carebridge.backend.community.entity.QuestionStatus.APPROVED
              )
            """)
    Page<CommunityAnswer> findVisibleToCommunity(Pageable pageable);

    @Query("""
            SELECT a FROM CommunityAnswer a
            WHERE a.status = :status
              AND NOT EXISTS (
                    SELECT r.id FROM ContentReport r
                    WHERE r.targetId = a.id
                      AND r.targetType = :targetType
                      AND r.status IN :openStatuses
              )
            """)
    Page<CommunityAnswer> findByStatusWithoutOpenModerationCase(
            @Param("status") AnswerStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("openStatuses") Collection<ReportStatus> openStatuses,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CommunityAnswer a WHERE a.id = :answerId")
    Optional<CommunityAnswer> findByIdForModerationUpdate(@Param("answerId") UUID answerId);

    // UC-199: fetch approved answers or author's own pending answers for question detail view
    List<CommunityAnswer> findAllByQuestionIdAndStatusOrderByCreatedAtDesc(UUID questionId, AnswerStatus status);

    @Query("""
            SELECT a FROM CommunityAnswer a
            WHERE a.questionId = :questionId
              AND (
                a.status = com.carebridge.backend.community.entity.AnswerStatus.APPROVED
                OR (:currentUserId IS NOT NULL AND a.authorId = :currentUserId AND a.status NOT IN (com.carebridge.backend.community.entity.AnswerStatus.DELETED, com.carebridge.backend.community.entity.AnswerStatus.HIDDEN))
              )
            ORDER BY a.createdAt DESC
            """)
    List<CommunityAnswer> findVisibleAnswersForDetail(
            @Param("questionId") UUID questionId,
            @Param("currentUserId") UUID currentUserId);

    List<CommunityAnswer> findAllByQuestionId(UUID questionId);

    // UC-111: dashboard aggregation — answer count grouped by status
    @Query("SELECT a.status, COUNT(a) FROM CommunityAnswer a GROUP BY a.status")
    List<Object[]> countGroupByStatus();

    // UC-111: dashboard aggregation — new answers within the reporting window
    long countByCreatedAtBetween(Instant from, Instant to);
}
