package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.health.dto.ShareSummaryRequest;
import com.carebridge.backend.health.dto.ShareSummaryResponse;
import com.carebridge.backend.health.repository.ConsultationBookingRepository;
import com.carebridge.backend.health.repository.DataPermissionRepository;
import com.carebridge.backend.health.repository.HealthSummaryRepository;
import com.carebridge.backend.health.service.IShareSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ShareSummaryServiceImpl implements IShareSummaryService {

    private final HealthSummaryRepository summaryRepository;
    private final ConsultationBookingRepository bookingRepository;
    private final DataPermissionRepository permissionRepository;
    private final AuditService auditService;

    @Override
    public ShareSummaryResponse shareSummary(ShareSummaryRequest request, UUID motherUserId) {
        // Gate 1: summary must be owned by caller
        summaryRepository.findByIdAndOwnerUserId(request.summaryId(), motherUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HEALTH-007: Summary not found or not owned by caller"));

        // Gate 2: booking must be ACTIVE (CONFIRMED/IN_PROGRESS) and owned by caller
        var booking = bookingRepository.findActiveByIdAndRequester(request.bookingId(), motherUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.valueOf(422), "HEALTH-008",
                        "HEALTH-008: Booking is not active or does not belong to caller"));

        // Gate 3: data_permission must be valid (ACTIVE, not expired) for this expert
        // We resolve the expert's userId via the booking's expertProfileId — here we check
        // whether any valid permission exists from mother to any grantee linked to this booking.
        // Implementation: check permission exists from motherUserId to the expert profile owner.
        boolean hasPermission = permissionRepository.existsValidPermission(
                motherUserId, booking.getExpertProfileId(), Instant.now());
        if (!hasPermission) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-009",
                    "HEALTH-009: No valid data permission granted for this expert");
        }

        // Update booking with shared summary ID
        bookingRepository.updateSharedSummaryId(request.bookingId(), request.summaryId());

        auditService.log(AuditAction.HEALTH_SUMMARY_SHARED, motherUserId,
                "HealthSummary", request.summaryId().toString(),
                "shared with booking=" + request.bookingId());

        log.info("Health summary shared: summaryId={}, bookingId={}, userId={}",
                request.summaryId(), request.bookingId(), motherUserId);

        return new ShareSummaryResponse(request.bookingId(), request.summaryId(), Instant.now());
    }
}
