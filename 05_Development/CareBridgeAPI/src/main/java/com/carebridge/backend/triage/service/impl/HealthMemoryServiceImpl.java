package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.HealthMemorySummaryPolicy;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.HealthMemoryProperties;
import com.carebridge.backend.triage.service.HealthMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HealthMemoryServiceImpl implements HealthMemoryService {

    private final HealthMemoryEntryRepository repository;
    private final IIntakeSessionRepository intakeSessionRepository;
    private final HealthMemorySummaryPolicy summaryPolicy;
    private final HealthMemoryProperties properties;

    @Autowired
    public HealthMemoryServiceImpl(
            HealthMemoryEntryRepository repository,
            IIntakeSessionRepository intakeSessionRepository,
            HealthMemorySummaryPolicy summaryPolicy,
            HealthMemoryProperties properties) {
        this.repository = repository;
        this.intakeSessionRepository = intakeSessionRepository;
        this.summaryPolicy = summaryPolicy;
        this.properties = properties;
    }

    /** Legacy convenience constructor (list/delete only — pre-THMC tests). */
    public HealthMemoryServiceImpl(HealthMemoryEntryRepository repository) {
        this(repository, null, new HealthMemorySummaryPolicy(), new HealthMemoryProperties());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthMemoryEntry> list(UUID userId, TriageStage stage, UUID profileId) {
        if (profileId == null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-014", "A matching profile is required for health memory");
        }
        Instant now = Instant.now();
        return stage.isMaternal()
                ? repository.findActiveMaternal(userId, profileId, stage, now)
                : repository.findActivePediatric(userId, profileId, stage, now);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID entryId) {
        HealthMemoryEntry entry = repository.findByIdAndUserIdAndDeletedAtIsNull(entryId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-015", "Health memory entry not found"));
        entry.setDeletedAt(Instant.now());
        repository.save(entry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<HealthMemoryEntry> writeFromCompletedSession(UUID sessionId, UUID userId) {
        // Owner-scoped reload (ADR-THMC-001 / L6): the event carries no stage/symptom data
        // and a missing/foreign session is a silent no-op, never a thrown TRIAGE-003.
        IntakeSession session = intakeSessionRepository.findByIdAndUserId(sessionId, userId).orElse(null);
        if (session == null
                || session.getStatus() != IntakeStatus.COMPLETED
                || session.getRiskLevel() == null
                || session.getCompletedAt() == null) {
            return Optional.empty(); // BR-THMC-001 status/risk gate
        }
        if (repository.existsBySourceSessionIdAndDeletedAtIsNull(sessionId)) {
            return Optional.empty(); // BR-THMC-001 idempotency guard (event replay)
        }
        TriageStage stage = session.getStage() == null ? TriageStage.INFANT : session.getStage();
        HealthMemoryEntry entry = HealthMemoryEntry.builder()
                .userId(session.getUserId())
                .motherProfileId(session.getMotherProfileId())
                .babyProfileId(session.getBabyProfileId())
                .relatedStage(stage)
                .summaryText(summaryPolicy.buildSummary(session))          // BR-THMC-003
                .memoryPayloadJson(summaryPolicy.buildPayloadJson(session)) // TDS §5.2 v1.0
                .sourceSessionId(session.getId())
                .createdAt(Instant.now())
                // BR-THMC-005 / ADR-THMC-002: absolute expiry anchored on completedAt, not now()
                .expiresAt(session.getCompletedAt().plus(properties.getTtlDays(), ChronoUnit.DAYS))
                .build();
        return Optional.of(repository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthMemoryContextItem> loadContextForIntake(UUID userId, TriageStage stage, UUID profileId) {
        if (profileId == null) {
            // Logic Issue L4: legacy sessions without a profile must not be blocked (no TRIAGE-014)
            return List.of();
        }
        Instant now = Instant.now();
        // C3: the guarded active queries are the SOLE read path (owner + profile + stage +
        // deleted_at IS NULL + expires_at > now, newest first) — never bypassed.
        List<HealthMemoryEntry> entries = stage.isMaternal()
                ? repository.findActiveMaternal(userId, profileId, stage, now)
                : repository.findActivePediatric(userId, profileId, stage, now);
        return entries.stream()
                .limit(properties.getMaxContextEntries())
                .map(this::toContextItem)
                .toList();
    }

    private HealthMemoryContextItem toContextItem(HealthMemoryEntry entry) {
        String summary = entry.getSummaryText() == null ? "" : entry.getSummaryText();
        int maxChars = properties.getMaxSummaryChars();
        if (summary.length() > maxChars) {
            summary = summary.substring(0, maxChars);
        }
        return new HealthMemoryContextItem(
                summary,
                entry.getRelatedStage() == null ? null : entry.getRelatedStage().name(),
                entry.getCreatedAt(),
                entry.getExpiresAt());
    }
}
