package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.PostpartumLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostpartumLogRepository extends JpaRepository<PostpartumLog, UUID> {
}
