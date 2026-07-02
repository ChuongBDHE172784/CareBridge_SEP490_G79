package com.carebridge.backend.content.service;

import com.carebridge.backend.consultation.repository.ConsultationSessionRepository;
import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import com.carebridge.backend.content.dto.response.ImpactReportResponse;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ImpactReportServiceImpl implements ImpactReportService {

    private static final String ANONYMIZATION_NOTE =
            "Aggregate-only, no personal data. Small-cohort suppression applies to any dimensional "
            + "breakdown (none in v1).";

    private final UserRepository userRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final PartnerOrganizationRepository partnerOrganizationRepository;
    private final ContentRepository contentRepository;

    @Override
    public ImpactReportResponse getImpactReport(ImpactReportFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw ModerationException.invalidImpactReportDateRange();
        }

        long mothersServed = userRepository.countByRole(Role.MOTHER);
        long consultationsDelivered = consultationSessionRepository.countByEndedAtIsNotNull();
        long activePartnerOrganizations = partnerOrganizationRepository.countByStatus(OrganizationStatus.APPROVED);
        long publishedContentItems = contentRepository.countByPublishedAtIsNotNull();

        // ADR-004 C4: suppression hook — inert in v1 (no dimensional breakdown to suppress).
        // System-wide totals are never suppressed; only future per-dimension breakdowns would need this.
        suppressSmallCohorts();

        return new ImpactReportResponse(
                mothersServed,
                consultationsDelivered,
                activePartnerOrganizations,
                publishedContentItems,
                filter.from(),
                filter.to(),
                Instant.now(),
                ANONYMIZATION_NOTE);
    }

    // ADR-004 C4 — small-cohort suppression scope guard. Inert in v1: no dimensional breakdown exists to
    // suppress. Kept as an explicit no-op hook so a future breakdown cannot ship without wiring through
    // here first (and without a DPO-approved k threshold — see TDS ADR-004).
    private void suppressSmallCohorts() {
        // No-op in v1 — see method Javadoc.
    }
}
