package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, UUID> {

    // ADR-COM-006: used to gate answer posting — only APPROVED questions accept answers
    Optional<CommunityQuestion> findByIdAndStatus(UUID id, QuestionStatus status);

    // UC-198: feed — all topics, APPROVED only, newest first
    Page<CommunityQuestion> findAllByStatusOrderByCreatedAtDesc(QuestionStatus status, Pageable pageable);

    Page<CommunityQuestion> findAllByStatusInOrderByCreatedAtDesc(
            java.util.Collection<QuestionStatus> statuses, Pageable pageable);

    // UC-198: feed — filtered by topic, APPROVED only, newest first
    Page<CommunityQuestion> findAllByStatusAndTopicIdOrderByCreatedAtDesc(
            QuestionStatus status, UUID topicId, Pageable pageable);

    Page<CommunityQuestion> findAllByStatusInAndTopicIdOrderByCreatedAtDesc(
            java.util.Collection<QuestionStatus> statuses, UUID topicId, Pageable pageable);

    // UC-162: search — APPROVED only, multi-filter, parameterized (ADR-COM-007, OWASP A03)
    @Query("""
            SELECT q FROM CommunityQuestion q
            WHERE q.status = com.carebridge.backend.community.entity.QuestionStatus.APPROVED
              AND (CAST(:keyword AS String) IS NULL
                   OR LOWER(q.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                   OR LOWER(q.body) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
              AND (:topicId IS NULL OR q.topicId = :topicId)
              AND (:stage IS NULL OR q.stage = :stage)
              AND (:hasExpertAnswer IS NULL
                   OR (:hasExpertAnswer = false)
                   OR (q.answerCount > 0 AND EXISTS (
                       SELECT a FROM CommunityAnswer a
                       WHERE a.questionId = q.id AND a.expertLabeled = true)))
            ORDER BY q.createdAt DESC
            """)
    Page<CommunityQuestion> searchApproved(
            @Param("keyword") String keyword,
            @Param("topicId") UUID topicId,
            @Param("stage") PregnancyStage stage,
            @Param("hasExpertAnswer") Boolean hasExpertAnswer,
            Pageable pageable);
}
