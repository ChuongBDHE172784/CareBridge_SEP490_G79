package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.impl.JourneyServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

/**
 * UC24 — ViewMotherJourneyDashboard service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class JourneyDashboardServiceImplTest {

    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private JourneyServiceImpl journeyService;

    @BeforeEach
    void setUp() {
        // Fixed clock = 2026-06-26 00:00 UTC (matches JourneyDashboardTestFactory.TODAY)
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ZoneOffset.UTC);
        journeyService = new JourneyServiceImpl(journeyRepository, userRepository, auditService, fixedClock);
    }

    /** TC-024-001: Active PREGNANCY journey → ACTIVE_PREGNANCY with week + trimester calculated. */
    @Test
    void getDashboard_activePregnancy_returnsCalculatedWeekAndTrimester() {
        var journey = JourneyDashboardTestFactory.makePregnancyJourney(20); // 20 weeks pregnant
        when(journeyRepository.findByOwnerUserIdAndStatus(
                JourneyDashboardTestFactory.MOTHER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(journey));

        JourneyDashboardResponse dashboard = journeyService.getDashboard(JourneyDashboardTestFactory.MOTHER_ID);

        assertThat(dashboard.getStatus()).isEqualTo(DashboardStatus.ACTIVE_PREGNANCY);
        assertThat(dashboard.getPregnancyWeek()).isEqualTo(20);
        assertThat(dashboard.getTrimester()).isEqualTo(2); // weeks 14–26 → T2
        assertThat(dashboard.getJourneyId()).isNotNull();
        assertThat(dashboard.getLastMenstrualDate()).isNotNull();
        assertThat(dashboard.getEstimatedDueDate()).isNotNull();
    }

    /** TC-024-002: Active POSTPARTUM journey → ACTIVE_POSTPARTUM, no pregnancy metrics. */
    @Test
    void getDashboard_activePostpartum_returnsPostpartumStatus() {
        var journey = JourneyDashboardTestFactory.makePostpartumJourney();
        when(journeyRepository.findByOwnerUserIdAndStatus(
                JourneyDashboardTestFactory.MOTHER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(journey));

        JourneyDashboardResponse dashboard = journeyService.getDashboard(JourneyDashboardTestFactory.MOTHER_ID);

        assertThat(dashboard.getStatus()).isEqualTo(DashboardStatus.ACTIVE_POSTPARTUM);
        assertThat(dashboard.getPregnancyWeek()).isNull();
        assertThat(dashboard.getTrimester()).isNull();
    }

    /**
     * TC-024-003: CRITICAL (Mobile onboarding) — No active journey must return
     * HTTP 200 with status=NO_JOURNEY (never throw a 404 exception).
     */
    @Test
    void getDashboard_noActiveJourney_returnsNoJourneyStatus_never404() {
        when(journeyRepository.findByOwnerUserIdAndStatus(
                JourneyDashboardTestFactory.MOTHER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // MUST NOT throw — assertThatNoException
        assertThatNoException().isThrownBy(() -> {
            JourneyDashboardResponse dashboard = journeyService.getDashboard(JourneyDashboardTestFactory.MOTHER_ID);
            assertThat(dashboard.getStatus()).isEqualTo(DashboardStatus.NO_JOURNEY);
            assertThat(dashboard.getJourneyId()).isNull();
            assertThat(dashboard.getPregnancyWeek()).isNull();
            assertThat(dashboard.getTrimester()).isNull();
            assertThat(dashboard.getDaysUntilDue()).isNull();
        });
    }

    /**
     * TC-024-004: Parameterized trimester boundary accuracy.
     * Trimesters: T1 = weeks ≤ 13, T2 = weeks 14–26, T3 = weeks ≥ 27.
     */
    @ParameterizedTest(name = "week {0} → trimester {1}")
    @CsvSource({
            "1,  1",   // week 1  → T1
            "13, 1",   // week 13 → T1 boundary
            "14, 2",   // week 14 → T2 boundary
            "26, 2",   // week 26 → T2 boundary
            "27, 3",   // week 27 → T3 boundary
            "40, 3"    // week 40 → T3 (post-due)
    })
    void getDashboard_pregnancyWeekTrimesterBoundaries(int weeksPregnant, int expectedTrimester) {
        var journey = JourneyDashboardTestFactory.makePregnancyJourney(weeksPregnant);
        when(journeyRepository.findByOwnerUserIdAndStatus(
                JourneyDashboardTestFactory.MOTHER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(journey));

        JourneyDashboardResponse dashboard = journeyService.getDashboard(JourneyDashboardTestFactory.MOTHER_ID);

        assertThat(dashboard.getPregnancyWeek()).isEqualTo(weeksPregnant);
        assertThat(dashboard.getTrimester()).isEqualTo(expectedTrimester);
        assertThat(dashboard.getStatus()).isEqualTo(DashboardStatus.ACTIVE_PREGNANCY);
    }
}
