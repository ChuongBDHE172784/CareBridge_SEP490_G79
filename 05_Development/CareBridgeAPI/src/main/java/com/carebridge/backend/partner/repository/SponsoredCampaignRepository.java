package com.carebridge.backend.partner.repository;
import com.carebridge.backend.partner.entity.SponsoredCampaign;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;import java.time.LocalDate;import java.util.List;
import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;
public interface SponsoredCampaignRepository extends JpaRepository<SponsoredCampaign,UUID> {
 @Query("select c.approvalStatus,count(c) from SponsoredCampaign c where c.partnerId=:partnerId and c.removed=false and (:from is null or c.endDate is null or c.endDate>=:from) and (:to is null or c.startDate is null or c.startDate<=:to) group by c.approvalStatus")
 List<Object[]> countByStatus(@Param("partnerId")UUID partnerId,@Param("from")LocalDate from,@Param("to")LocalDate to);
 Page<SponsoredCampaign> findByPartnerIdAndRemovedFalse(UUID partnerId,Pageable pageable);
}
