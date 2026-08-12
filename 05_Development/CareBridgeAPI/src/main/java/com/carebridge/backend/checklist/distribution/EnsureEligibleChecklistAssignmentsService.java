package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Selects the startup-configured candidate authority and isolates each materialization attempt. */
@Service
public class EnsureEligibleChecklistAssignmentsService {

    private static final Logger log = LoggerFactory.getLogger(EnsureEligibleChecklistAssignmentsService.class);

    private final ChecklistReconciliationSource source;
    private final EnsureEligibleChecklistAssignmentExecutor executor;

    public EnsureEligibleChecklistAssignmentsService(
            ChecklistReconciliationSource source,
            EnsureEligibleChecklistAssignmentExecutor executor) {
        this.source = source;
        this.executor = executor;
    }

    public void ensureEligibleAssignments(
            UUID actorUserId,
            LocalDate effectiveDate,
            ZoneId timezone,
            UUID correlationId) {
        source.loadCandidatesForActor(actorUserId, effectiveDate, timezone, correlationId).stream()
                .sorted(Comparator.comparing(EnsureEligibleChecklistAssignmentsService::signature))
                .forEach(candidate -> executeIsolated(candidate, correlationId));
    }

    /** Replays a bounded weekly horizon as closed, non-actionable History. */
    public void ensureCatchUpAssignments(
            UUID actorUserId,
            LocalDate asOfDate,
            ZoneId timezone,
            UUID correlationId,
            int weeklyPeriods) {
        if (actorUserId == null || asOfDate == null || timezone == null || correlationId == null) {
            return;
        }
        int boundedPeriods = Math.max(0, Math.min(weeklyPeriods, 12));
        for (int offset = boundedPeriods; offset >= 1; offset--) {
            LocalDate periodDate = asOfDate.minusWeeks(offset);
            source.loadCandidatesForActor(actorUserId, periodDate, timezone, correlationId).stream()
                    .filter(candidate -> candidate.cadence() != null)
                    .map(candidate -> candidate.withCadence(candidate.cadence().catchUp()))
                    .sorted(Comparator.comparing(EnsureEligibleChecklistAssignmentsService::signature))
                    .forEach(candidate -> executeIsolated(candidate, correlationId));
        }
    }

    private void executeIsolated(ChecklistDistributionCommand candidate, UUID correlationId) {
        try {
            executor.execute(candidate);
        } catch (RuntimeException exception) {
            log.warn(
                    "checklist_materialization_candidate_failed correlationId={} candidateKeyHash={} templateVersionId={} contextType={} failureCode={}",
                    correlationId, hash(signature(candidate)), candidate.templateVersionId(), candidate.contextType(),
                    failureCode(exception));
        }
    }

    static String signature(ChecklistDistributionCommand candidate) {
        String substage = candidate.substage() == null ? "<ABSENT>" : String.join("|",
                token(candidate.substage().getStage()), token(candidate.substage().getAnchorType()),
                token(candidate.substage().getRangeUnit()), token(candidate.substage().getStartInclusive()),
                token(candidate.substage().getEndInclusive()));
        String recipients = candidate.recipients().stream()
                .sorted(Comparator.comparing(recipient -> token(recipient.userId()) + "|" + token(recipient.role())))
                .map(recipient -> String.join("|", token(recipient.userId()), token(recipient.role()),
                        token(recipient.acceptedMembership()), token(recipient.checklistView()),
                        token(recipient.checklistComplete())))
                .reduce((left, right) -> left + "," + right).orElse("");
        String items = candidate.items().stream()
                .sorted(Comparator.comparing(item -> token(item.templateItemVersionId())))
                .map(item -> String.join("|", token(item.templateItemVersionId()), token(item.displayOrder()),
                        token(item.required()), token(item.targetSubject()), token(item.dueAnchor()),
                        token(item.dueOffsetDays()), token(item.dueOffsetUnit())))
                .reduce((left, right) -> left + "," + right).orElse("");
        String cadence = candidate.cadence() == null ? "<ABSENT>"
                : String.join("|", token(candidate.cadence().scheduleType()),
                        token(candidate.cadence().materializationPolicy()),
                        token(candidate.cadence().periodKey()),
                        token(candidate.cadence().scheduleZone()),
                        token(candidate.cadence().materializationMode()),
                        token(candidate.cadence().wasActionable()));
        return String.join("|", token(candidate.templateLineageId()), token(candidate.templateVersionId()),
                token(candidate.careGroupId()), token(candidate.careGroupOwnerUserId()),
                token(candidate.contextType()), token(candidate.contextId()), token(candidate.contextOwnerUserId()),
                token(candidate.stage()), token(candidate.gestationalDatingRevision()), substage, recipients, items,
                cadence);
    }

    private static String token(Object value) {
        if (value == null) {
            return "<ABSENT>";
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof ChecklistRecipientRole role) {
            return role.name();
        }
        return value.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String failureCode(RuntimeException exception) {
        return exception instanceof IllegalArgumentException ? "INVALID_CANDIDATE" : "MATERIALIZATION_FAILED";
    }
}
