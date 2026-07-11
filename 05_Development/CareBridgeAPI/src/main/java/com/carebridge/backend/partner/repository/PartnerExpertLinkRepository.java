package com.carebridge.backend.partner.repository;
import com.carebridge.backend.partner.entity.PartnerExpertLink;import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface PartnerExpertLinkRepository extends JpaRepository<PartnerExpertLink,UUID>{long countByPartnerIdAndStatus(UUID partnerId,String status);}
