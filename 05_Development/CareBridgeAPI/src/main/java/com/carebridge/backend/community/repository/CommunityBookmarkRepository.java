package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityBookmarkRepository extends JpaRepository<CommunityBookmark, UUID> {

    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);

    Optional<CommunityBookmark> findByUserIdAndQuestionId(UUID userId, UUID questionId);

    Page<CommunityBookmark> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
