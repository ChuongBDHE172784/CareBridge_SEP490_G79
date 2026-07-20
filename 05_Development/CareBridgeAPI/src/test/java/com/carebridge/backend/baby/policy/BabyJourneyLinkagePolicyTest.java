package com.carebridge.backend.baby.policy;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.repository.*;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BabyJourneyLinkagePolicyTest {
    @Mock UserRepository users;
    @Mock MotherJourneyRepository journeys;
    @Mock PregnancyOutcomeEvidenceRepository evidence;
    @InjectMocks BabyJourneyLinkagePolicy policy;
    UUID owner=UUID.randomUUID(), journeyId=UUID.randomUUID();

    @Test void acceptsOnlyOwnedCanonicalActivePostpartumWithMatchingLiveBirthEvidence() {
        var journey=MotherJourney.builder().id(journeyId).ownerUserId(owner).status(JourneyStatus.ACTIVE).journeyType(JourneyType.POSTPARTUM).pregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH).build();
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().id(owner).role(Role.MOTHER).build()));
        when(journeys.findCanonicalForUpdate(owner)).thenReturn(Optional.of(journey));
        when(evidence.findFirstByJourneyIdOrderByRevisionNumberDesc(journeyId)).thenReturn(Optional.of(PregnancyOutcomeEvidence.builder().journeyId(journeyId).ownerUserId(owner).outcomeType(PregnancyOutcomeType.LIVE_BIRTH).build()));
        assertThat(policy.requireEligibleJourney(journeyId, owner)).isSameAs(journey);
    }

    @Test void missingEvidenceFailsClosed() {
        var journey=MotherJourney.builder().id(journeyId).ownerUserId(owner).status(JourneyStatus.ACTIVE).journeyType(JourneyType.POSTPARTUM).pregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH).build();
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().id(owner).role(Role.MOTHER).build()));
        when(journeys.findCanonicalForUpdate(owner)).thenReturn(Optional.of(journey));
        when(evidence.findFirstByJourneyIdOrderByRevisionNumberDesc(journeyId)).thenReturn(Optional.empty());
        assertThatThrownBy(()->policy.requireEligibleJourney(journeyId,owner)).isInstanceOf(BusinessException.class)
                .satisfies(e->assertThat(((BusinessException)e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test void foreignJourneyUsesNeutralNotFound() {
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().id(owner).role(Role.MOTHER).build()));
        when(journeys.findCanonicalForUpdate(owner)).thenReturn(Optional.empty());
        assertThatThrownBy(()->policy.requireEligibleJourney(journeyId,owner)).isInstanceOf(BusinessException.class)
                .satisfies(e->assertThat(((BusinessException)e).getCode()).isEqualTo("LINK_RESOURCE_NOT_FOUND"));
    }

    @Test void readUsesCanonicalMaternalLifecycleRatherThanNewestActiveJourney() {
        var journey=MotherJourney.builder().id(journeyId).ownerUserId(owner).status(JourneyStatus.ACTIVE).journeyType(JourneyType.POSTPARTUM).pregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH).build();
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().id(owner).role(Role.MOTHER).build()));
        when(journeys.findById(journeyId)).thenReturn(Optional.of(journey));
        when(journeys.findCanonical(owner)).thenReturn(Optional.of(journey));
        when(evidence.findFirstByJourneyIdOrderByRevisionNumberDesc(journeyId)).thenReturn(Optional.of(PregnancyOutcomeEvidence.builder().journeyId(journeyId).ownerUserId(owner).outcomeType(PregnancyOutcomeType.LIVE_BIRTH).build()));

        assertThat(policy.requireEligibleJourneyForRead(journeyId, owner)).isSameAs(journey);

        verify(journeys, never()).findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(any(), any());
    }
}
