package com.carebridge.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.dto.response.CommunityDashboardResponse;
import com.carebridge.backend.content.dto.response.TrendingTopic;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.service.CommunityDashboardServiceImpl;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityDashboardServiceImplTest {

    @Mock
    private com.carebridge.backend.security.repository.UserRepository userRepository;

    @Mock
    private CommunityQuestionRepository communityQuestionRepository;

    @Mock
    private CommunityAnswerRepository communityAnswerRepository;

    @Mock
    private ContentReportRepository contentReportRepository;

    @InjectMocks
    private CommunityDashboardServiceImpl service;

    private void stubAllEmpty() {
        when(userRepository.countGroupByRole()).thenReturn(List.of());
        when(userRepository.countActive(any())).thenReturn(0L);
        when(communityQuestionRepository.countGroupByStatus()).thenReturn(List.of());
        when(communityQuestionRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(communityQuestionRepository.findTrendingTopics(any(), any(), any())).thenReturn(List.of());
        when(communityAnswerRepository.countGroupByStatus()).thenReturn(List.of());
        when(communityAnswerRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(contentReportRepository.countGroupByStatus()).thenReturn(List.of());
        when(contentReportRepository.findResolvedTimestamps()).thenReturn(List.of());
    }

    // DASH-TC-101
    @Test
    void getDashboard_happyPath_assemblesAllMetrics() {
        when(userRepository.countGroupByRole()).thenReturn(CommunityDashboardTestFactory.roleCounts());
        when(userRepository.countActive(any())).thenReturn(1240L);
        when(communityQuestionRepository.countGroupByStatus())
                .thenReturn(CommunityDashboardTestFactory.questionStatusCounts());
        when(communityQuestionRepository.countByCreatedAtBetween(any(), any())).thenReturn(210L);
        when(communityQuestionRepository.findTrendingTopics(any(), any(), any())).thenReturn(List.of());
        when(communityAnswerRepository.countGroupByStatus())
                .thenReturn(CommunityDashboardTestFactory.answerStatusCounts());
        when(communityAnswerRepository.countByCreatedAtBetween(any(), any())).thenReturn(640L);
        when(contentReportRepository.countGroupByStatus())
                .thenReturn(CommunityDashboardTestFactory.reportStatusCounts());
        when(contentReportRepository.findResolvedTimestamps()).thenReturn(List.of());

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        // Derived from the factory so the expectation cannot drift when a role is added.
        long expectedUserTotal = CommunityDashboardTestFactory.expectedRoleMap()
                .values().stream().mapToLong(Long::longValue).sum();
        assertEquals(expectedUserTotal, response.userMetrics().total());
        assertEquals(1240L, response.userMetrics().active());
        assertEquals(3300L + 12L + 80L + 8L, response.questionMetrics().total());
        assertEquals(210L, response.questionMetrics().newInPeriod());
        assertEquals(9700L + 30L + 70L, response.answerMetrics().total());
        assertEquals(640L, response.answerMetrics().newInPeriod());
        assertThat(response.generatedAt()).isNotNull();
    }

    // DASH-TC-102
    @Test
    void getDashboard_userMetricsByRole_matchesFactoryCounts() {
        stubAllEmpty();
        when(userRepository.countGroupByRole()).thenReturn(CommunityDashboardTestFactory.roleCounts());

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(CommunityDashboardTestFactory.expectedRoleMap(), response.userMetrics().byRole());
    }

    @Test
    void getDashboard_nullRoleGroup_isReportedAsUnassigned() {
        stubAllEmpty();
        when(userRepository.countGroupByRole()).thenReturn(List.of(
                new Object[] {null, 10L},
                new Object[] {com.carebridge.backend.security.rbac.Role.SYSTEM_ADMIN, 2L}));

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(12L, response.userMetrics().total());
        assertEquals(10L, response.userMetrics().byRole().get("UNASSIGNED"));
        assertEquals(2L, response.userMetrics().byRole().get("SYSTEM_ADMIN"));
    }

    // DASH-TC-103
    @Test
    void getDashboard_activeUsers_excludesSuspendedLockedDisabled() {
        stubAllEmpty();
        when(userRepository.countActive(any())).thenReturn(4L);

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(4L, response.userMetrics().active());
    }

    // DASH-TC-104
    @Test
    void getDashboard_questionMetrics_byStatusAndNewInPeriod() {
        stubAllEmpty();
        when(communityQuestionRepository.countGroupByStatus())
                .thenReturn(CommunityDashboardTestFactory.questionStatusCounts());
        when(communityQuestionRepository.countByCreatedAtBetween(any(), any())).thenReturn(210L);

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(4, response.questionMetrics().byStatus().size());
        assertEquals(210L, response.questionMetrics().newInPeriod());
    }

    // DASH-TC-105
    @Test
    void getDashboard_noResolvedReports_avgHandlingTimeIsNull() {
        stubAllEmpty();
        when(contentReportRepository.findResolvedTimestamps()).thenReturn(List.of());

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertNull(response.reportMetrics().avgHandlingTimeSeconds());
    }

    // DASH-TC-106
    @Test
    void getDashboard_resolvedReports_avgHandlingTimeComputedCorrectly() {
        stubAllEmpty();
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        List<Object[]> timestamps = List.of(
                new Object[] {base, base.plusSeconds(3600)},
                new Object[] {base, base.plusSeconds(10800)});
        when(contentReportRepository.findResolvedTimestamps()).thenReturn(timestamps);

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(7200.0, response.reportMetrics().avgHandlingTimeSeconds(), 0.001);
    }

    @Test
    void getDashboard_negativeReportDurations_areIgnored() {
        stubAllEmpty();
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        List<Object[]> timestamps = List.of(
                new Object[] {base, base.minusSeconds(3600)},
                new Object[] {base, base.plusSeconds(1800)});
        when(contentReportRepository.findResolvedTimestamps()).thenReturn(timestamps);

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(1800.0, response.reportMetrics().avgHandlingTimeSeconds(), 0.001);
    }

    // DASH-TC-107
    @Test
    void getDashboard_trendingTopics_mapsRepositoryResultInOrder() {
        stubAllEmpty();
        UUID topicA = UUID.randomUUID();
        UUID topicB = UUID.randomUUID();
        List<Object[]> trending = List.of(
                new Object[] {topicA, "Dinh dưỡng thai kỳ", 58L},
                new Object[] {topicB, "Chăm sóc sau sinh", 30L});
        when(communityQuestionRepository.findTrendingTopics(any(), any(), any())).thenReturn(trending);

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertThat(response.trendingTopics()).containsExactly(
                new TrendingTopic(topicA, "Dinh dưỡng thai kỳ", 58L),
                new TrendingTopic(topicB, "Chăm sóc sau sinh", 30L));
    }

    // DASH-TC-108
    @Test
    void getDashboard_invalidRange_throwsMod021() {
        DashboardFilter filter = new DashboardFilter(LocalDate.parse("2026-06-30"), LocalDate.parse("2026-06-01"));

        ModerationException ex = assertThrows(ModerationException.class, () -> service.getDashboard(filter));

        assertEquals("MOD-021", ex.getCode());
    }

    // DASH-TC-109
    @Test
    void getDashboard_emptyDatabase_allZeroNoCrash() {
        stubAllEmpty();

        CommunityDashboardResponse response = service.getDashboard(CommunityDashboardTestFactory.makeFilter());

        assertEquals(0L, response.userMetrics().total());
        assertEquals(0L, response.userMetrics().active());
        assertEquals(0L, response.questionMetrics().total());
        assertEquals(0L, response.answerMetrics().total());
        assertNull(response.reportMetrics().avgHandlingTimeSeconds());
        assertThat(response.trendingTopics()).isEmpty();
    }

    // DASH-TC-110 — CRITICAL PDPA gate
    @Test
    void communityDashboardResponse_containsNoRowLevelPiiFields() {
        for (var recordType : List.of(
                CommunityDashboardResponse.class,
                com.carebridge.backend.content.dto.response.UserMetrics.class,
                com.carebridge.backend.content.dto.response.ContentCountMetrics.class,
                com.carebridge.backend.content.dto.response.ReportMetrics.class,
                TrendingTopic.class)) {
            for (var field : recordType.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                assertThat(name).as("field %s.%s must not look like row-level PII", recordType.getSimpleName(), name)
                        .doesNotContain("email")
                        .doesNotContain("phone")
                        .doesNotContain("username")
                        .doesNotContain("body")
                        .doesNotContain("title");
                assertThat(field.getType().getPackageName())
                        .as("field %s.%s must not be a JPA entity type", recordType.getSimpleName(), name)
                        .doesNotContain(".entity");
            }
        }
    }
}
