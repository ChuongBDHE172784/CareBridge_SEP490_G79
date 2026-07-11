package com.carebridge.backend.partner.repository;

import com.carebridge.backend.partner.entity.PartnerService;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PartnerServiceRepository extends JpaRepository<PartnerService, UUID> {
    @Query("select s.approvalStatus, count(s) from PartnerService s where s.partnerId=:partnerId and s.removed=false group by s.approvalStatus")
    List<Object[]> countByStatus(@Param("partnerId") UUID partnerId);
}
