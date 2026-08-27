package com.carebridge.backend.carejourney.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One growth measuring session: the weight, height and head circumference recorded together
 * on a single date.
 *
 * <p>Wave 13 cutover (V3 §3.12): this is no longer a JPA entity. Growth readings live in
 * {@code health_observations} as up to three rows sharing a {@code measurement_group_id};
 * {@link com.carebridge.backend.carejourney.repository.GrowthMeasurementStore} projects them
 * back into this session shape, which is what the DTOs, the controller and the chart have
 * always been built from. Instances handed out by the store are detached projections, not
 * managed entities — mutating one changes nothing until it is passed back to
 * {@code GrowthMeasurementStore#save}.
 *
 * <p>{@code growth_measurements} is frozen and awaiting its contract migration; nothing here
 * reads or writes it any more.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthMeasurement {

    /** Identity of the session. Stored as {@code health_observations.measurement_group_id}. */
    private UUID growthMeasurementId;

    /**
     * Retained because the service and DTOs address a baby by this id. The canonical column
     * is {@code care_subject_id}; the two were identical in every row at migration time, and
     * {@link #alignCanonicalCareSubject()} keeps them mirrored for callers that set only one.
     */
    private UUID babyId;

    private UUID careSubjectId;

    private LocalDate measuredDate;

    private BigDecimal weightKg;

    private BigDecimal heightCm;

    private BigDecimal headCircumferenceCm;

    /** Where the measurement was taken (HOME, CLINIC, HOME_SCALE) — not how it reached us. */
    private String sourceType;

    private String note;

    private Instant deletedAt;

    private Instant createdAt;

    private Instant updatedAt;

    public void alignCanonicalCareSubject() {
        if (careSubjectId == null && babyId != null) {
            careSubjectId = babyId;
        }
        if (babyId == null && careSubjectId != null) {
            babyId = careSubjectId;
        }
    }
}
