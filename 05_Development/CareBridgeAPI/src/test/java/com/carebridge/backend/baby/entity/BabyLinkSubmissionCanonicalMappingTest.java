package com.carebridge.backend.baby.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BabyLinkSubmissionCanonicalMappingTest {

    @Test
    void operationBecomesCanonicalAuditCategory() {
        String semanticIntent = "intent-".repeat(100);
        UUID submissionId = UUID.randomUUID();
        BabyLinkSubmission submission = BabyLinkSubmission.builder()
                .ownerUserId(UUID.randomUUID())
                .operationType(BabyLinkOperation.LINK_EXISTING)
                .submissionId(submissionId)
                .semanticIntent(semanticIntent)
                .babyId(UUID.randomUUID())
                .journeyId(UUID.randomUUID())
                .build();

        submission.prepareCanonicalEvent();

        assertThat(submission.getEventCategory()).isEqualTo("BABY_LINK_LINK_EXISTING");
        assertThat(submission.getPayload())
                .containsEntry("operationType", "LINK_EXISTING")
                .containsEntry("submissionId", submissionId)
                .containsEntry("semanticIntent", semanticIntent);

        submission.setSubmissionId(null);
        submission.setSemanticIntent(null);
        submission.setOperationType(null);
        submission.hydrateCanonicalEvent();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getSemanticIntent()).isEqualTo(semanticIntent);
        assertThat(submission.getOperationType()).isEqualTo(BabyLinkOperation.LINK_EXISTING);
    }
}
