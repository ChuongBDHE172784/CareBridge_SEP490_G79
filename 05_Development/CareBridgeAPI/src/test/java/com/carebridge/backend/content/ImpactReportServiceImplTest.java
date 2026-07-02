package com.carebridge.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.repository.ConsultationSessionRepository;
import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import com.carebridge.backend.content.dto.response.ImpactReportResponse;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ImpactReportServiceImpl;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImpactReportServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConsultationSessionRepository consultationSessionRepository;

    @Mock
    private PartnerOrganizationRepository partnerOrganizationRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ImpactReportServiceImpl service;

    private void stubAllZero() {
        when(userRepository.countByRole(Role.MOTHER)).thenReturn(0L);
        when(consultationSessionRepository.countByEndedAtIsNotNull()).thenReturn(0L);
        when(partnerOrganizationRepository.countByStatus(OrganizationStatus.APPROVED)).thenReturn(0L);
        when(contentRepository.countByPublishedAtIsNotNull()).thenReturn(0L);
    }

    // IMP-TC-101
    @Test
    void getImpactReport_happyPath_assemblesAllMetrics() {
        when(userRepository.countByRole(Role.MOTHER)).thenReturn(ImpactReportTestFactory.MOTHERS);
        when(consultationSessionRepository.countByEndedAtIsNotNull()).thenReturn(ImpactReportTestFactory.CONSULTATIONS);
        when(partnerOrganizationRepository.countByStatus(OrganizationStatus.APPROVED))
                .thenReturn(ImpactReportTestFactory.ACTIVE_PARTNERS);
        when(contentRepository.countByPublishedAtIsNotNull()).thenReturn(ImpactReportTestFactory.PUBLISHED_CONTENT);

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(ImpactReportTestFactory.MOTHERS, response.mothersServed());
        assertEquals(ImpactReportTestFactory.CONSULTATIONS, response.consultationsDelivered());
        assertEquals(ImpactReportTestFactory.ACTIVE_PARTNERS, response.activePartnerOrganizations());
        assertEquals(ImpactReportTestFactory.PUBLISHED_CONTENT, response.publishedContentItems());
        assertEquals(ImpactReportTestFactory.FROM, response.periodFrom());
        assertEquals(ImpactReportTestFactory.TO, response.periodTo());
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.anonymizationNote()).isNotBlank();
    }

    // IMP-TC-102
    @Test
    void getImpactReport_mothersServed_queriesOnlyMotherRole() {
        stubAllZero();
        when(userRepository.countByRole(Role.MOTHER)).thenReturn(500L);

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(500L, response.mothersServed());
        verify(userRepository).countByRole(eq(Role.MOTHER));
    }

    // IMP-TC-103
    @Test
    void getImpactReport_consultationsDelivered_usesEndedAtNotNullOracle() {
        stubAllZero();
        when(consultationSessionRepository.countByEndedAtIsNotNull()).thenReturn(77L);

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(77L, response.consultationsDelivered());
    }

    // IMP-TC-104
    @Test
    void getImpactReport_activePartnerOrganizations_countsOnlyApproved() {
        stubAllZero();
        when(partnerOrganizationRepository.countByStatus(OrganizationStatus.APPROVED)).thenReturn(12L);

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(12L, response.activePartnerOrganizations());
        verify(partnerOrganizationRepository).countByStatus(eq(OrganizationStatus.APPROVED));
    }

    // IMP-TC-105
    @Test
    void getImpactReport_publishedContentItems_countsOnlyPublished() {
        stubAllZero();
        when(contentRepository.countByPublishedAtIsNotNull()).thenReturn(88L);

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(88L, response.publishedContentItems());
    }

    // IMP-TC-106
    @Test
    void getImpactReport_emptyDatabase_allZeroNoCrash() {
        stubAllZero();

        ImpactReportResponse response = service.getImpactReport(ImpactReportTestFactory.makeFilter());

        assertEquals(0L, response.mothersServed());
        assertEquals(0L, response.consultationsDelivered());
        assertEquals(0L, response.activePartnerOrganizations());
        assertEquals(0L, response.publishedContentItems());
        assertThat(response.anonymizationNote()).isNotBlank();
    }

    // IMP-TC-107 (also exercised via ImpactReportControllerTest at the HTTP layer)
    @Test
    void getImpactReport_invalidRange_throwsMod022() {
        ImpactReportFilter filter = new ImpactReportFilter(LocalDate.parse("2026-06-30"), LocalDate.parse("2026-01-01"));

        ModerationException ex = assertThrows(ModerationException.class, () -> service.getImpactReport(filter));

        assertEquals("MOD-022", ex.getCode());
        verify(userRepository, never()).countByRole(org.mockito.ArgumentMatchers.any());
    }

    // IMP-TC-108 — CRITICAL PDPA gate (external-facing, stricter than UC-111)
    @Test
    void impactReportResponse_fieldsAreAllPrimitiveOrDateNoPii() {
        for (Field field : ImpactReportResponse.class.getDeclaredFields()) {
            String name = field.getName().toLowerCase();
            assertThat(name)
                    .as("field %s must not look like row-level PII", name)
                    .doesNotContain("name")
                    .doesNotContain("email")
                    .doesNotContain("phone")
                    .doesNotContain("id")
                    .doesNotContain("address");

            Class<?> type = field.getType();
            boolean allowedType = type.isPrimitive()
                    || type == String.class
                    || type == java.time.LocalDate.class
                    || type == java.time.Instant.class;
            assertThat(allowedType)
                    .as("field %s type %s must be primitive/String/date — no entity or collection", name, type)
                    .isTrue();
        }
    }

    // IMP-TC-109 — CRITICAL suppression scope guard (ADR-004 tripwire)
    @Test
    void impactReportResponse_hasNoDimensionalBreakdownField() {
        List<String> fieldNames = java.util.Arrays.stream(ImpactReportResponse.class.getDeclaredFields())
                .map(Field::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(fieldNames)
                .as("v1 must not ship an un-thresholded dimensional breakdown (ADR-004 — needs DPO-approved k first)")
                .noneMatch(name -> name.contains("region") || name.contains("byrole")
                        || name.contains("bypartner") || name.contains("breakdown"));
    }
}
