package com.carebridge.backend.triage;

import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.EvidenceSourceRepository;
import com.carebridge.backend.triage.repository.EvidenceSourceReviewLogRepository;
import com.carebridge.backend.triage.service.impl.EvidenceSourceServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceSourceServiceImplTest {
    private final EvidenceSourceRepository sources = mock(EvidenceSourceRepository.class);
    private final EvidenceSourceReviewLogRepository logs = mock(EvidenceSourceReviewLogRepository.class);
    private final EvidenceSourceServiceImpl service = new EvidenceSourceServiceImpl(sources, logs);

    @Test
    void proposerCannotApproveTheirOwnEvidenceSource() {
        UUID proposer = UUID.randomUUID();
        EvidenceSource source = source("PENDING_REVIEW", proposer);
        when(sources.findById(source.getId())).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.changeStatus(
                source.getId(), "APPROVED", "looks good", proposer, "ADMIN"))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("EVIDENCE-006");
        verify(sources, never()).save(any());
    }

    @Test
    void statusChangesFollowTheClosedReviewWorkflow() {
        UUID reviewer = UUID.randomUUID();
        EvidenceSource source = source("PENDING_REVIEW", UUID.randomUUID());
        when(sources.findById(source.getId())).thenReturn(Optional.of(source));
        when(sources.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceSource approved = service.changeStatus(
                source.getId(), "approved", "reviewed", reviewer, "ADMIN");

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getReviewedBy()).isEqualTo(reviewer);
        verify(logs).save(any());
    }

    @Test
    void arbitraryOrBackwardStatusChangeIsRejected() {
        EvidenceSource source = source("APPROVED", UUID.randomUUID());
        when(sources.findById(source.getId())).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.changeStatus(
                source.getId(), "PENDING_REVIEW", null, UUID.randomUUID(), "ADMIN"))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("EVIDENCE-005");
        verify(sources, never()).save(any());
    }

    @Test
    void proposalRejectsPrivateAndReservedNetworkDestinations() {
        for (String url : java.util.List.of(
                "https://127.0.0.1", "https://[fc00::1]", "https://192.0.2.1")) {
            assertThatThrownBy(() -> service.propose(
                    url, "test", "GUIDELINE", "PREGNANCY", null, UUID.randomUUID()))
                    .isInstanceOf(TriageException.class)
                    .extracting("code").isEqualTo("EVIDENCE-001");
        }
        verify(sources, never()).save(any());
    }

    private EvidenceSource source(String status, UUID proposer) {
        return EvidenceSource.builder()
                .id(UUID.randomUUID())
                .domain("who.int")
                .baseUrl("https://who.int")
                .organization("WHO")
                .category("GUIDELINE")
                .status(status)
                .discoveryMode("MANUAL_ADMIN_ADD")
                .applicableStages("PREGNANCY")
                .addedBy(proposer)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
