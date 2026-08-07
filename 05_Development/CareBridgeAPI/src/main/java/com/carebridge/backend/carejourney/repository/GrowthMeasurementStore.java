package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes growth measuring sessions on {@code health_observations}.
 *
 * <p>Wave 13 cutover (V3 §3.12): {@code growth_measurements} is no longer read or written.
 * One session is up to three observation rows sharing a {@code measurement_group_id}; this
 * class is the only place that knows it. Callers keep working with
 * {@link GrowthMeasurement}, the session shape the DTOs and the growth chart are built from.
 *
 * <p>Mapping rationale, including why the note goes to {@code context_jsonb} rather than the
 * misleadingly named {@code HealthObservation#note} (which is {@code text_value}, the
 * observation's own textual content), is recorded in
 * {@code 08_References/Wave13_Growth_To_Observations_Mapping.md}.
 */
@Component
@RequiredArgsConstructor
public class GrowthMeasurementStore {

    /** Marks the rows this store owns, and keeps them out of every maternal query. */
    public static final String LEGACY_SOURCE = "growth_measurements";

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String WEIGHT = "BABY_WEIGHT";
    private static final String HEIGHT = "BABY_HEIGHT";
    private static final String HEAD = "BABY_HEAD_CIRCUMFERENCE";
    private static final List<String> TYPES = List.of(WEIGHT, HEIGHT, HEAD);
    private static final String SETTING_KEY = "measurementSetting";
    private static final String NOTE_KEY = "note";

    private final HealthObservationRepository observationRepository;

    @Transactional(readOnly = true)
    public List<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(UUID babyId) {
        return sessionsOf(babyId, false).stream()
                .sorted(Comparator.comparing(GrowthMeasurement::getMeasuredDate))
                .toList();
    }

    /** Includes soft-deleted sessions; used where history must show everything ever recorded. */
    @Transactional(readOnly = true)
    public List<GrowthMeasurement> findByBabyIdOrderByMeasuredDateAsc(UUID babyId) {
        return sessionsOf(babyId, true).stream()
                .sorted(Comparator.comparing(GrowthMeasurement::getMeasuredDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(
            UUID babyId, Pageable pageable) {
        List<GrowthMeasurement> all = sessionsOf(babyId, false).stream()
                .sorted(Comparator.comparing(GrowthMeasurement::getMeasuredDate).reversed())
                .toList();
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

    @Transactional(readOnly = true)
    public Optional<GrowthMeasurement> findById(UUID growthMeasurementId) {
        List<HealthObservation> rows =
                observationRepository.findGrowthByMeasurementGroup(LEGACY_SOURCE, growthMeasurementId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(toSession(rows));
    }

    @Transactional(readOnly = true)
    public Optional<GrowthMeasurement> findByGrowthMeasurementIdAndBabyId(
            UUID growthMeasurementId, UUID babyId) {
        return findById(growthMeasurementId)
                .filter(session -> babyId.equals(session.getCareSubjectId()));
    }

    /**
     * Writes a session as up to three observations.
     *
     * <p>A measurement that is absent from the session is soft-deleted rather than removed,
     * so its {@code legacy_id} keeps its place and a rerun of the backfill cannot resurrect
     * it. Deleting the whole session soft-deletes every row in the group.
     */
    @Transactional
    public GrowthMeasurement save(GrowthMeasurement session) {
        session.alignCanonicalCareSubject();
        if (session.getGrowthMeasurementId() == null) {
            session.setGrowthMeasurementId(UUID.randomUUID());
        }
        Instant now = Instant.now();

        Map<String, HealthObservation> existing = new LinkedHashMap<>();
        for (HealthObservation row : observationRepository.findGrowthByMeasurementGroup(
                LEGACY_SOURCE, session.getGrowthMeasurementId())) {
            existing.put(row.getMetricCode(), row);
        }

        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(WEIGHT, session.getWeightKg());
        values.put(HEIGHT, session.getHeightCm());
        values.put(HEAD, session.getHeadCircumferenceCm());

        List<HealthObservation> toSave = new ArrayList<>();
        for (String type : TYPES) {
            BigDecimal value = values.get(type);
            HealthObservation row = existing.get(type);

            if (value == null) {
                // Nothing recorded for this measurement. Only an existing row needs action,
                // and the action is a soft delete — see the class javadoc.
                if (row != null && row.getDeletedAt() == null) {
                    row.setDeletedAt(now);
                    toSave.add(row);
                }
                continue;
            }

            if (row == null) {
                row = HealthObservation.builder()
                        .careSubjectId(session.getCareSubjectId())
                        .metricCode(type)
                        .subjectType("BABY")
                        .sourceType(DataSource.MANUAL)
                        .measurementGroupId(session.getGrowthMeasurementId())
                        .legacySource(LEGACY_SOURCE)
                        .legacyId(session.getGrowthMeasurementId() + ":" + type)
                        .build();
            }
            row.setValueNumeric(value);
            row.setUnit(WEIGHT.equals(type) ? "kg" : "cm");
            row.setMeasuredAt(session.getMeasuredDate().atStartOfDay(ZONE).toInstant());
            row.setContext(contextOf(session));
            // Re-entering a measurement revives the session's row rather than leaving a
            // deleted one behind under the same legacy_id.
            row.setDeletedAt(session.getDeletedAt());
            toSave.add(row);
        }

        if (session.getDeletedAt() != null) {
            // Deleting the session takes the whole group with it, including rows whose value
            // was not restated in this call.
            for (HealthObservation row : existing.values()) {
                row.setDeletedAt(session.getDeletedAt());
                if (!toSave.contains(row)) {
                    toSave.add(row);
                }
            }
        }

        observationRepository.saveAll(toSave);
        return findById(session.getGrowthMeasurementId()).orElse(session);
    }

    private List<GrowthMeasurement> sessionsOf(UUID careSubjectId, boolean includeDeleted) {
        Map<UUID, List<HealthObservation>> byGroup = new LinkedHashMap<>();
        for (HealthObservation row : observationRepository.findGrowthByCareSubject(
                LEGACY_SOURCE, careSubjectId)) {
            byGroup.computeIfAbsent(row.getMeasurementGroupId(), key -> new ArrayList<>()).add(row);
        }
        List<GrowthMeasurement> sessions = new ArrayList<>();
        for (List<HealthObservation> rows : byGroup.values()) {
            GrowthMeasurement session = toSession(rows);
            if (includeDeleted || session.getDeletedAt() == null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    /**
     * A session is live while any of its measurements is; it is deleted only once every row
     * is. Treating a partially deleted group as deleted would hide readings the user still
     * has.
     */
    private GrowthMeasurement toSession(Collection<HealthObservation> rows) {
        GrowthMeasurement session = new GrowthMeasurement();
        Instant latestDeletion = null;
        boolean allDeleted = true;

        for (HealthObservation row : rows) {
            session.setGrowthMeasurementId(row.getMeasurementGroupId());
            session.setCareSubjectId(row.getCareSubjectId());
            session.setBabyId(row.getCareSubjectId());
            session.setMeasuredDate(LocalDate.ofInstant(row.getMeasuredAt(), ZONE));
            Map<String, Object> context = row.getContext() == null ? Map.of() : row.getContext();
            session.setSourceType(asString(context.get(SETTING_KEY)));
            session.setNote(asString(context.get(NOTE_KEY)));
            session.setCreatedAt(earliest(session.getCreatedAt(), row.getCreatedAt()));
            session.setUpdatedAt(latest(session.getUpdatedAt(), row.getUpdatedAt()));

            if (row.getDeletedAt() == null) {
                allDeleted = false;
            } else {
                latestDeletion = latest(latestDeletion, row.getDeletedAt());
            }

            if (row.getDeletedAt() != null) {
                continue;
            }
            switch (row.getMetricCode()) {
                case WEIGHT -> session.setWeightKg(row.getValueNumeric());
                case HEIGHT -> session.setHeightCm(row.getValueNumeric());
                case HEAD -> session.setHeadCircumferenceCm(row.getValueNumeric());
                default -> { /* not a growth measurement; the query already excludes these */ }
            }
        }

        session.setDeletedAt(allDeleted ? latestDeletion : null);
        return session;
    }

    private static Map<String, Object> contextOf(GrowthMeasurement session) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (session.getSourceType() != null) {
            context.put(SETTING_KEY, session.getSourceType());
        }
        if (session.getNote() != null) {
            context.put(NOTE_KEY, session.getNote());
        }
        return context;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Instant earliest(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private static Instant latest(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isAfter(current) ? candidate : current;
    }
}
