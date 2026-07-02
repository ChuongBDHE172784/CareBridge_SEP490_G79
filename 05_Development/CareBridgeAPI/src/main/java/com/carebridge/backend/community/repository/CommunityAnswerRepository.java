package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, UUID> {

    // UC-198: batch-check which questions in a set have at least one expert-labeled answer
    @Query("SELECT DISTINCT a.questionId FROM CommunityAnswer a WHERE a.questionId IN :questionIds AND a.expertLabeled = true")
    Set<UUID> findQuestionIdsWithExpertAnswer(java.util.Collection<UUID> questionIds);

    // UC-199: fetch approved answers for a question detail view
    List<CommunityAnswer> findAllByQuestionIdAndStatusOrderByCreatedAtDesc(UUID questionId, AnswerStatus status);

    // UC-111: dashboard aggregation — answer count grouped by status
    @Query("SELECT a.status, COUNT(a) FROM CommunityAnswer a GROUP BY a.status")
    List<Object[]> countGroupByStatus();

    // UC-111: dashboard aggregation — new answers within the reporting window
    long countByCreatedAtBetween(Instant from, Instant to);
}
