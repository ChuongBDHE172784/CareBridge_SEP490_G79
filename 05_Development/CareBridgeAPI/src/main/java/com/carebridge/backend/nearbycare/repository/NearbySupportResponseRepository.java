package com.carebridge.backend.nearbycare.repository;

import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NearbySupportResponseRepository extends JpaRepository<NearbySupportResponse, UUID> {

    List<NearbySupportResponse> findByRequestId(UUID requestId);

    List<NearbySupportResponse> findByExpertProfileId(UUID expertProfileId);
}
