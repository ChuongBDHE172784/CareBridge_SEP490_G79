package com.carebridge.backend.partner.repository;

import com.carebridge.backend.partner.entity.PartnerService;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartnerServiceRepository extends JpaRepository<PartnerService, UUID> {
    @Query("select s.approvalStatus, count(s) from PartnerService s where s.partnerId=:partnerId and s.removed=false group by s.approvalStatus")
    List<Object[]> countByStatus(@Param("partnerId") UUID partnerId);

    Page<PartnerService> findByPartnerIdAndRemovedFalse(UUID partnerId, Pageable pageable);
}
