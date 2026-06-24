package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, UUID> {

    // ADR-COM-006: used to gate answer posting — only APPROVED questions accept answers
    Optional<CommunityQuestion> findByIdAndStatus(UUID id, QuestionStatus status);

    // UC-198: feed — all topics, APPROVED only, newest first
    Page<CommunityQuestion> findAllByStatusOrderByCreatedAtDesc(QuestionStatus status, Pageable pageable);

    // UC-198: feed — filtered by topic, APPROVED only, newest first
    Page<CommunityQuestion> findAllByStatusAndTopicIdOrderByCreatedAtDesc(
            QuestionStatus status, UUID topicId, Pageable pageable);
}
