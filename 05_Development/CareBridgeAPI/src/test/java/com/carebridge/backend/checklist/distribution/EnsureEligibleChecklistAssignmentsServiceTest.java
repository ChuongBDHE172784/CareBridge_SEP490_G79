package com.carebridge.backend.checklist.distribution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnsureEligibleChecklistAssignmentsServiceTest {

    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CORRELATION = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 31);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void loadsRequestCandidatesForActor() {
        ChecklistReconciliationSource source = mock(ChecklistReconciliationSource.class);
        EnsureEligibleChecklistAssignmentExecutor executor = mock(EnsureEligibleChecklistAssignmentExecutor.class);
        ChecklistDistributionCommand candidate = command(50);
        when(source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION)).thenReturn(List.of(candidate));

        service(source, executor).ensureEligibleAssignments(ACTOR, DATE, ZONE, CORRELATION);

        verify(source).loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION);
        verify(executor).execute(candidate);
    }

    @Test
    void executesCandidatesInDeterministicSignatureOrder() {
        ChecklistReconciliationSource source = mock(ChecklistReconciliationSource.class);
        EnsureEligibleChecklistAssignmentExecutor executor = mock(EnsureEligibleChecklistAssignmentExecutor.class);
        ChecklistDistributionCommand later = command(90);
        ChecklistDistributionCommand first = command(10);
        when(source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION))
                .thenReturn(List.of(later, first));

        service(source, executor).ensureEligibleAssignments(ACTOR, DATE, ZONE, CORRELATION);

        var ordered = inOrder(executor);
        ordered.verify(executor).execute(first);
        ordered.verify(executor).execute(later);
    }

    @Test
    void candidateFailureIsIsolatedAndLaterCandidateContinues() {
        ChecklistReconciliationSource source = mock(ChecklistReconciliationSource.class);
        EnsureEligibleChecklistAssignmentExecutor executor = mock(EnsureEligibleChecklistAssignmentExecutor.class);
        ChecklistDistributionCommand later = command(90);
        ChecklistDistributionCommand first = command(10);
        when(source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION))
                .thenReturn(List.of(later, first));
        org.mockito.Mockito.doThrow(new IllegalStateException("synthetic"))
                .when(executor).execute(first);

        service(source, executor).ensureEligibleAssignments(ACTOR, DATE, ZONE, CORRELATION);

        var ordered = inOrder(executor);
        ordered.verify(executor).execute(first);
        ordered.verify(executor).execute(later);
    }

    private static EnsureEligibleChecklistAssignmentsService service(
            ChecklistReconciliationSource source,
            EnsureEligibleChecklistAssignmentExecutor executor) {
        return new EnsureEligibleChecklistAssignmentsService(source, executor);
    }

    private static ChecklistDistributionCommand command(int suffix) {
        return new ChecklistDistributionCommand(
                uuid(300 + suffix), uuid(100 + suffix), null, ACTOR,
                ChecklistCareContextType.JOURNEY, uuid(200 + suffix), ACTOR,
                null, null, new ChecklistLifecycleDates(null, null, null, null),
                DATE, ZONE,
                List.of(new ChecklistDistributionRecipient(
                        ACTOR, ChecklistRecipientRole.MOTHER, true, true, true)),
                List.of(), CORRELATION);
    }

    private static UUID uuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }
}
