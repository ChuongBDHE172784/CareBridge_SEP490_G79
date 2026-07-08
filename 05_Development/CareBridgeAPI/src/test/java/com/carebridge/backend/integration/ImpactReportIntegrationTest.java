package com.carebridge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.consultation.entity.ConsultationSession;
import com.carebridge.backend.consultation.repository.ConsultationSessionRepository;
import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import com.carebridge.backend.content.dto.response.ImpactReportResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ImpactReportService;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// IMP-TC-INT-001, IMP-TC-INT-002
// Note: no Testcontainers/real-Postgres harness exists in this codebase (verified project-wide, same
// finding as UC-110/UC-111's integration tests). Hosted as @SpringBootTest + H2 (real Spring-managed
// beans end-to-end) instead — seeded rows here ARE the oracle (equivalent to a direct-SQL cross-check).
@SpringBootTest
@Transactional
class ImpactReportIntegrationTest {

    @Autowired
    private ImpactReportService impactReportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultationSessionRepository consultationSessionRepository;

    @Autowired
    private PartnerOrganizationRepository partnerOrganizationRepository;

    @Autowired
    private ContentRepository contentRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    private void makeUser(Role role, String phoneSuffix) {
        userRepository.save(User.builder().phone("08" + phoneSuffix).role(role).build());
    }

    private void makeSession(Instant endedAt) {
        ConsultationSession session = new ConsultationSession();
        session.setEndedAt(endedAt);
        session.setCreatedAt(Instant.now());
        consultationSessionRepository.save(session);
    }

    private void makePartner(OrganizationStatus status, String emailSuffix) {
        partnerOrganizationRepository.save(PartnerOrganization.builder()
                .name("Partner " + emailSuffix)
                .type(OrganizationType.CLINIC)
                .address("123 Test St")
                .city("Hanoi")
                .phone("0900000000")
                .email("partner" + emailSuffix + "@example.com")
                .status(status)
                .representativeUserId(UUID.randomUUID())
                .build());
    }

    private void makeContent(Instant publishedAt) {
        contentRepository.save(ContentItem.builder()
                .type(ContentType.ARTICLE)
                .title("Test content")
                .status(ContentStatus.APPROVED)
                .publishedAt(publishedAt)
                .build());
    }

    // IMP-TC-INT-001
    @Test
    void getImpactReport_seededData_countsMatchKnownSeed() {
        makeUser(Role.MOTHER, "11111111");
        makeUser(Role.MOTHER, "22222222");
        makeUser(Role.EXPERT, "33333333"); // excluded — not MOTHER

        makeSession(Instant.now()); // ended
        makeSession(null); // not ended — excluded

        makePartner(OrganizationStatus.APPROVED, "1");
        makePartner(OrganizationStatus.PENDING_APPROVAL, "2"); // excluded

        makeContent(Instant.now()); // published
        makeContent(null); // draft — excluded

        ImpactReportResponse response = impactReportService.getImpactReport(new ImpactReportFilter(null, null));

        assertThat(response.mothersServed()).isEqualTo(2L);
        assertThat(response.consultationsDelivered()).isEqualTo(1L);
        assertThat(response.activePartnerOrganizations()).isEqualTo(1L);
        assertThat(response.publishedContentItems()).isEqualTo(1L);
        assertThat(response.anonymizationNote()).isNotBlank();
    }

    // IMP-TC-INT-002
    @Test
    void getImpactReport_readOnly_doesNotMutateAnyTable() {
        makeUser(Role.MOTHER, "44444444");
        makeSession(Instant.now());
        makePartner(OrganizationStatus.APPROVED, "9");
        makeContent(Instant.now());

        long usersBefore = userRepository.count();
        long sessionsBefore = consultationSessionRepository.count();
        long partnersBefore = partnerOrganizationRepository.count();
        long contentBefore = contentRepository.count();

        impactReportService.getImpactReport(new ImpactReportFilter(null, null));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(consultationSessionRepository.count()).isEqualTo(sessionsBefore);
        assertThat(partnerOrganizationRepository.count()).isEqualTo(partnersBefore);
        assertThat(contentRepository.count()).isEqualTo(contentBefore);
    }
}
