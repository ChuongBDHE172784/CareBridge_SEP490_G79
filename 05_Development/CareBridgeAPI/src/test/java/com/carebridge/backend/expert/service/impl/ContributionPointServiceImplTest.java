package com.carebridge.backend.expert.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.expert.repository.ContributionPointRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContributionPointServiceImplTest {

    @Mock private ContributionPointRepository repository;

    @Test
    void breakdownUsesUserScopedAggregateAndNormalizesMissingSourceTypes() {
        UUID userId = UUID.randomUUID();
        when(repository.sumPointsGroupedBySourceType(userId)).thenReturn(List.of(
                total("EXPERT_ANSWER", 7L),
                total(null, 2L),
                total(" ", 3L),
                total("CREDENTIAL", 5L)));
        ContributionPointServiceImpl service = new ContributionPointServiceImpl(repository);

        Map<String, Integer> result = service.getBreakdownBySourceType(userId);

        assertThat(result).containsExactly(
                Map.entry("CREDENTIAL", 5),
                Map.entry("EXPERT_ANSWER", 7),
                Map.entry("UNSPECIFIED", 5));
        verify(repository).sumPointsGroupedBySourceType(userId);
        verify(repository, never()).findAll();
    }

    @Test
    void breakdownForMissingUserIsEmptyWithoutQueryingTheDatabase() {
        ContributionPointServiceImpl service = new ContributionPointServiceImpl(repository);

        assertThat(service.getBreakdownBySourceType(null)).isEmpty();

        verifyNoInteractions(repository);
    }

    private static ContributionPointRepository.SourceTypeTotal total(
            String sourceType, Long totalPoints) {
        return new ContributionPointRepository.SourceTypeTotal() {
            @Override
            public String getSourceType() {
                return sourceType;
            }

            @Override
            public Long getTotalPoints() {
                return totalPoints;
            }
        };
    }
}
