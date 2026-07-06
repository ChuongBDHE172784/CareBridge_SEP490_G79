package com.carebridge.backend.nearbycare.repository;

import com.carebridge.backend.nearbycare.entity.NearbySupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NearbySupportRequestRepository extends JpaRepository<NearbySupportRequest, UUID> {

    List<NearbySupportRequest> findByRequesterUserId(UUID requesterUserId);

    List<NearbySupportRequest> findByStatus(NearbySupportRequest.SupportStatus status);
}
