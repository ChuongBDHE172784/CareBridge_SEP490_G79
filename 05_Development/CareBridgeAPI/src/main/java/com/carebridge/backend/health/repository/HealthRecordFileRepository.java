package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthRecordFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HealthRecordFileRepository extends JpaRepository<HealthRecordFile, UUID> {

    List<HealthRecordFile> findByHealthRecordIdOrderByDisplayOrderAsc(UUID healthRecordId);

    List<HealthRecordFile> findByFileId(UUID fileId);
}
