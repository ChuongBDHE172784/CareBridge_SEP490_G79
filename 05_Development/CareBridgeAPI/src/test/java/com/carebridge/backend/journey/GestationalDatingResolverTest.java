package com.carebridge.backend.journey;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.service.GestationalDatingResolution;
import com.carebridge.backend.journey.service.GestationalDatingResolver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestationalDatingResolverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 18);
    private final GestationalDatingResolver resolver = new GestationalDatingResolver();

    @Test
    void v1LmpOnlyResolvesCanonicalAnchorAndPlan() {
        CreateJourneyRequest request = pregnancy();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));

        GestationalDatingResolution result = resolver.resolveCreate(request, 1, TODAY);

        assertThat(result.basis()).isEqualTo(GestationalDatingBasis.LMP);
        assertThat(result.canonicalLmp()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.estimatedDueDate()).isEqualTo(LocalDate.of(2027, 3, 8));
        assertThat(result.completedGestationalWeek()).isEqualTo(6);
        assertThat(result.sourceWeekNumber()).isEqualTo(7);
        assertThat(result.plan()).isEqualTo(1);
    }

    @Test
    void v1EddOnlyResolvesWithoutFabricatingSourceLmp() {
        CreateJourneyRequest request = pregnancy();
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 8));

        GestationalDatingResolution result = resolver.resolveCreate(request, 1, TODAY);

        assertThat(result.basis()).isEqualTo(GestationalDatingBasis.EDD);
        assertThat(result.lastMenstrualDate()).isNull();
        assertThat(result.canonicalLmp()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void v1ExactPairUsesLmpAuthority() {
        CreateJourneyRequest request = pregnancy();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 8));

        assertThat(resolver.resolveCreate(request, 1, TODAY).basis())
                .isEqualTo(GestationalDatingBasis.LMP);
    }

    @Test
    void v1NonExactPairRemainsUnresolved() {
        CreateJourneyRequest request = pregnancy();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 7));

        GestationalDatingResolution result = resolver.resolveCreate(request, 1, TODAY);

        assertThat(result.resolved()).isFalse();
        assertThat(result.basis()).isNull();
        assertThat(result.lastMenstrualDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.estimatedDueDate()).isEqualTo(LocalDate.of(2027, 3, 7));
    }

    @Test
    void v1PregnancyWithoutDatingStartsUnresolvedInsteadOfThrowing() {
        GestationalDatingResolution result = resolver.resolveCreate(pregnancy(), 1, TODAY);

        assertThat(result.resolved()).isFalse();
        assertThat(result.datingScope()).isFalse();
    }

    @Test
    void v2RequiresMatchingXor() {
        CreateJourneyRequest both = pregnancy();
        both.setDatingBasis(GestationalDatingBasis.LMP);
        both.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        both.setEstimatedDueDate(LocalDate.of(2027, 3, 8));

        assertThatThrownBy(() -> resolver.resolveCreate(both, 2, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exactly one");

        CreateJourneyRequest noDate = pregnancy();
        noDate.setDatingBasis(GestationalDatingBasis.LMP);
        assertThatThrownBy(() -> resolver.resolveCreate(noDate, 2, TODAY))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void futureCanonicalLmpFailsClosed() {
        CreateJourneyRequest request = pregnancy();
        request.setLastMenstrualDate(TODAY.plusDays(1));

        assertThatThrownBy(() -> resolver.resolveCreate(request, 1, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("future");
    }

    @Test
    void planBoundariesUseOneBasedSourceWeek() {
        assertThat(GestationalDatingResolver.planForSourceWeek(20)).isEqualTo(1);
        assertThat(GestationalDatingResolver.planForSourceWeek(21)).isEqualTo(2);
        assertThat(GestationalDatingResolver.planForSourceWeek(25)).isEqualTo(2);
        assertThat(GestationalDatingResolver.planForSourceWeek(26)).isEqualTo(3);
        assertThat(GestationalDatingResolver.planForSourceWeek(40)).isEqualTo(8);
    }

    @Test
    void resolvedV1CorrectionRequiresAuthoritativeDate() {
        var current = com.carebridge.backend.journey.entity.MotherJourney.builder()
                .journeyType(JourneyType.PREGNANCY)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(1L)
                .gestationalDatingEffectiveAt(Instant.parse("2026-07-17T00:00:00Z"))
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build();
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setEstimatedDueDate(LocalDate.of(2027, 3, 9));

        assertThatThrownBy(() -> resolver.resolveUpdate(current, request, 1, TODAY, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contract version 2");
    }

    @Test
    void v2SameAuthorityAnchorIsSemanticNoOp() {
        var current = com.carebridge.backend.journey.entity.MotherJourney.builder()
                .journeyType(JourneyType.PREGNANCY)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(1L)
                .gestationalDatingEffectiveAt(Instant.parse("2026-07-17T00:00:00Z"))
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build();
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setChecklistContractVersion(2);
        request.setDatingBasis(GestationalDatingBasis.LMP);
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));

        GestationalDatingResolution result = resolver.resolveUpdate(
                current, request, 2, TODAY, false);

        assertThat(result.resolved()).isTrue();
        assertThat(result.semanticNoOp()).isTrue();
        assertThat(result.canonicalLmp()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void incompleteAuthorityFailsClosedWhenNoDatingIsSubmitted() {
        var current = com.carebridge.backend.journey.entity.MotherJourney.builder()
                .journeyType(JourneyType.PREGNANCY)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build();

        GestationalDatingResolution result = resolver.resolveUpdate(
                current, new UpdateJourneyRequest(), 1, TODAY, false);

        assertThat(result.resolved()).isFalse();
        assertThat(result.canonicalLmp()).isNull();
    }

    @Test
    void v2SameRawDatesCanInitializeAnIncompleteAuthorityTuple() {
        var current = com.carebridge.backend.journey.entity.MotherJourney.builder()
                .journeyType(JourneyType.PREGNANCY)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build();
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setChecklistContractVersion(2);
        request.setDatingBasis(GestationalDatingBasis.LMP);
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));

        GestationalDatingResolution result = resolver.resolveUpdate(
                current, request, 2, TODAY, false);

        assertThat(result.resolved()).isTrue();
        assertThat(result.semanticNoOp()).isFalse();
    }

    private CreateJourneyRequest pregnancy() {
        CreateJourneyRequest request = new CreateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setDateSource(com.carebridge.backend.journey.entity.JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(com.carebridge.backend.journey.entity.JourneyDateConfidence.ESTIMATED);
        return request;
    }
}
