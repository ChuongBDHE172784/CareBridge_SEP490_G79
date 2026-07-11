package com.carebridge.backend.partner.service;
import com.carebridge.backend.partner.dto.request.PerformanceFilter;import com.carebridge.backend.partner.dto.response.PartnerPerformanceResponse;import java.util.UUID;
public interface PartnerPerformanceService{PartnerPerformanceResponse getPerformance(PerformanceFilter filter,UUID actorId);}
