package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, UUID> {
}
