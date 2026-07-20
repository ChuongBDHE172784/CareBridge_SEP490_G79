package com.carebridge.backend.triage;

import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.service.impl.HealthMemoryServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class HealthMemoryServiceImplTest {

    @Mock
    private HealthMemoryEntryRepository repository;

    @Test
    void delete_shouldSoftDeleteEntryAndExcludeItFromSubsequentReads() {
        UUID userId = UUID.randomUUID();
        UUID babyProfileId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        HealthMemoryEntry entry = HealthMemoryEntry.builder()
                .id(entryId)
                .userId(userId)
                .babyProfileId(babyProfileId)
                .relatedStage(TriageStage.INFANT)
                .summaryText("Processed summary only")
                .createdAt(Instant.now())
                .build();
        HealthMemoryServiceImpl service = new HealthMemoryServiceImpl(repository);

        when(repository.findByIdAndUserIdAndDeletedAtIsNull(entryId, userId)).thenReturn(Optional.of(entry));
        when(repository.save(any(HealthMemoryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findActivePediatric(eq(userId), eq(babyProfileId), eq(TriageStage.INFANT), any(Instant.class)))
                .thenAnswer(invocation -> entry.getDeletedAt() == null ? List.of(entry) : List.of());

        service.delete(userId, entryId);

        assertThat(entry.getDeletedAt()).isNotNull();
        assertThat(service.list(userId, TriageStage.INFANT, babyProfileId)).isEmpty();
    }

    @Test
    void postpartum_shouldUseMaternalMemoryBoundaryOnly() {
        UUID userId = UUID.randomUUID();
        UUID motherProfileId = UUID.randomUUID();
        HealthMemoryServiceImpl service = new HealthMemoryServiceImpl(repository);
        when(repository.findActiveMaternal(
                eq(userId), eq(motherProfileId), eq(TriageStage.POSTPARTUM), any(Instant.class)))
                .thenReturn(List.of());

        assertThat(service.list(userId, TriageStage.POSTPARTUM, motherProfileId)).isEmpty();

        verify(repository).findActiveMaternal(
                eq(userId), eq(motherProfileId), eq(TriageStage.POSTPARTUM), any(Instant.class));
        verify(repository, never()).findActivePediatric(any(), any(), any(), any());
    }
}
