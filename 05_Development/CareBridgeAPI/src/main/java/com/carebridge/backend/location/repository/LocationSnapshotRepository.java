package com.carebridge.backend.location.repository;

import com.carebridge.backend.location.entity.LocationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LocationSnapshotRepository extends JpaRepository<LocationSnapshot, UUID> {
    List<LocationSnapshot> findByUserId(UUID userId);
}
