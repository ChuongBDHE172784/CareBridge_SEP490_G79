package com.carebridge.backend.partner.dto.response;
import java.time.*;import java.util.Map;
public record PartnerPerformanceResponse(Map<String,Long> serviceListingsByStatus,Map<String,Long> campaignsByStatus,long activeExpertLinks,LocalDate periodFrom,LocalDate periodTo,Instant generatedAt){}
