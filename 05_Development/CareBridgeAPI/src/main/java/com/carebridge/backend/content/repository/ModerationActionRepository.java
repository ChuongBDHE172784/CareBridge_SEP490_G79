package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ModerationAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {
}
