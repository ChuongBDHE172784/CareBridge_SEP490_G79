package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.TodayTaskContextLabelResolver;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TodayTaskContextLabelResolverTest {

    @Test
    void personalJourneyLabelDoesNotRequireCareGroup() {
        UUID journeyId = UUID.randomUUID();
        CareGroupRepository groups = mock(CareGroupRepository.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(journeys.findAllById(any())).thenReturn(List.of(MotherJourney.builder()
                .id(journeyId).journeyType(JourneyType.PREGNANCY).status(JourneyStatus.ACTIVE).build()));
        TodayTaskContextLabelResolver resolver = new TodayTaskContextLabelResolver(groups, journeys, babies);
        TodayTaskCandidate candidate = new TodayTaskCandidate(
                TaskKind.CHECKLIST, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                ChecklistCareContextType.JOURNEY, journeyId, "Personal task",
                ChecklistTargetSubject.MOTHER, ChecklistOrigin.SYSTEM_TEMPLATE,
                "PENDING", Set.of(), null);

        var labels = resolver.resolve(List.of(candidate));

        assertThat(labels).hasSize(1);
        assertThat(labels.entrySet()).singleElement().satisfies(entry -> {
            assertThat(entry.getKey().careGroupId()).isNull();
            assertThat(entry.getValue().careGroupLabel()).isNull();
            assertThat(entry.getValue().careContextLabel()).isEqualTo("Mang thai");
        });
    }
}
