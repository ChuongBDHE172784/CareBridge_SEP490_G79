package com.carebridge.backend.journey.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Pure, server-side implementation of AD-32's V1/V2 dating matrix.
 *
 * <p>No database, caller effective time, display timezone, or clinical
 * discrepancy threshold is consulted here.  The service supplies the
 * server-current schedule-zone date and persists the returned authority.</p>
 */
@Component
public class GestationalDatingResolver {

    public static final int V1 = 1;
    public static final int V2 = 2;
    public static final int GESTATION_DAYS = 280;

    /**
     * A row is eligible to project a current Plan only when the complete
     * server-owned authority tuple is present and the journey is active.
     */
    public static boolean hasResolvedAuthority(MotherJourney journey) {
        return journey != null
                && journey.getJourneyType() == JourneyType.PREGNANCY
                && journey.getStatus() == JourneyStatus.ACTIVE
                && journey.getGestationalDatingBasis() != null
                && journey.getGestationalDatingRevision() != null
                && journey.getGestationalDatingRevision() > 0
                && journey.getGestationalDatingEffectiveAt() != null
                && journey.getGestationalDatingQuarantineReasonCode() == null;
    }

    public GestationalDatingResolution resolveCreate(
            CreateJourneyRequest request,
            int contractVersion,
            LocalDate serverToday) {
        Objects.requireNonNull(request, "request");
        JourneyType stage = request.getJourneyType();
        boolean hasDating = hasDating(request.getLastMenstrualDate(), request.getEstimatedDueDate(),
                request.getDatingBasis());
        if (stage != JourneyType.PREGNANCY) {
            if (hasDating) {
                throw stageInapplicable();
            }
            return GestationalDatingResolution.unresolved(null, null, false);
        }
        GestationalDatingResolution resolved = resolveShape(
                request.getLastMenstrualDate(),
                request.getEstimatedDueDate(),
                request.getDatingBasis(),
                contractVersion,
                null,
                null,
                serverToday,
                hasDating,
                false,
                false);
        return resolved;
    }

    public GestationalDatingResolution resolveUpdate(
            MotherJourney current,
            UpdateJourneyRequest request,
            int contractVersion,
            LocalDate serverToday,
            boolean enteringPregnancy) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(request, "request");
        JourneyType resultingStage = request.getJourneyType() == null
                ? current.getJourneyType()
                : request.getJourneyType();
        boolean hasDating = hasDating(request.getLastMenstrualDate(), request.getEstimatedDueDate(),
                request.getDatingBasis());
        boolean datingScope = hasDating || enteringPregnancy;
        if (resultingStage != JourneyType.PREGNANCY) {
            if (hasDating) {
                throw stageInapplicable();
            }
            return GestationalDatingResolution.unresolved(
                    current.getLastMenstrualDate(), current.getEstimatedDueDate(), false);
        }

        if (!datingScope) {
            return currentResolution(current, serverToday);
        }

        // A new epoch must not inherit the previous pregnancy's source dates or
        // authority.  V1 may deliberately start unresolved; V2 must provide a
        // fresh XOR authority.
        if (enteringPregnancy) {
            return resolveShape(
                    request.getLastMenstrualDate(),
                    request.getEstimatedDueDate(),
                    request.getDatingBasis(),
                    contractVersion,
                    null,
                    null,
                    serverToday,
                    hasDating || enteringPregnancy,
                    true,
                    false);
        }

        GestationalDatingResolution resolved = resolveShape(
                request.getLastMenstrualDate(),
                request.getEstimatedDueDate(),
                request.getDatingBasis(),
                contractVersion,
                current.getGestationalDatingBasis(),
                canonicalLmp(
                        current.getGestationalDatingQuarantineReasonCode() == null
                                ? current.getGestationalDatingBasis() : null,
                        current.getLastMenstrualDate(),
                        current.getEstimatedDueDate()),
                serverToday,
                hasDating,
                false,
                hasResolvedAuthority(current));
        if (contractVersion == V2
                && hasResolvedAuthority(current)
                && resolved.resolved()
                && resolved.basis() == current.getGestationalDatingBasis()
                && Objects.equals(
                        resolved.canonicalLmp(),
                        canonicalLmp(
                                current.getGestationalDatingBasis(),
                                current.getLastMenstrualDate(),
                                current.getEstimatedDueDate()))) {
            return GestationalDatingResolution.noOp(
                    resolved.basis(),
                    resolved.lastMenstrualDate(),
                    resolved.estimatedDueDate(),
                    resolved.canonicalLmp(),
                    resolved.completedGestationalWeek(),
                    resolved.completedGestationalDays(),
                    resolved.sourceWeekNumber(),
                    resolved.plan());
        }
        return resolved;
    }

    /** Returns the canonical anchor for a resolved source shape. */
    public static LocalDate canonicalLmp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate) {
        if (basis == null) {
            return null;
        }
        return switch (basis) {
            case LMP -> lastMenstrualDate;
            case EDD, LMP_DERIVED_FROM_EDD -> estimatedDueDate == null
                    ? null
                    : estimatedDueDate.minusDays(GESTATION_DAYS);
        };
    }

    /** Zero-based completed week, evaluated against the server schedule date. */
    public static int completedGestationalWeek(LocalDate canonicalLmp, LocalDate serverToday) {
        if (canonicalLmp == null || serverToday == null) {
            return -1;
        }
        long days = ChronoUnit.DAYS.between(canonicalLmp, serverToday);
        if (days < 0) {
            throw futureLmp();
        }
        return Math.toIntExact(days / 7);
    }

    /** Remainder days (0 to 6) in the current gestational week. */
    public static int completedGestationalDays(LocalDate canonicalLmp, LocalDate serverToday) {
        if (canonicalLmp == null || serverToday == null) {
            return -1;
        }
        long days = ChronoUnit.DAYS.between(canonicalLmp, serverToday);
        if (days < 0) {
            throw futureLmp();
        }
        return Math.toIntExact(days % 7);
    }

    /** One-based source week used by the WHO Plan labels. */
    public static int sourceWeekNumber(int completedGestationalWeek) {
        return completedGestationalWeek < 0 ? -1 : completedGestationalWeek + 1;
    }

    /** Plan labels are intentionally source-facing and start Plan 2 at week 21. */
    public static int planForSourceWeek(int sourceWeekNumber) {
        if (sourceWeekNumber < 1) {
            throw new IllegalArgumentException("sourceWeekNumber must be positive");
        }
        if (sourceWeekNumber <= 20) return 1;
        if (sourceWeekNumber <= 25) return 2;
        if (sourceWeekNumber <= 29) return 3;
        if (sourceWeekNumber <= 33) return 4;
        if (sourceWeekNumber <= 35) return 5;
        if (sourceWeekNumber <= 37) return 6;
        if (sourceWeekNumber <= 39) return 7;
        return 8;
    }

    private GestationalDatingResolution currentResolution(
            MotherJourney current,
            LocalDate serverToday) {
        if (!hasResolvedAuthority(current)) {
            return GestationalDatingResolution.unresolved(
                    current.getLastMenstrualDate(), current.getEstimatedDueDate(), false);
        }
        GestationalDatingBasis basis = current.getGestationalDatingBasis();
        LocalDate canonical = canonicalLmp(
                basis, current.getLastMenstrualDate(), current.getEstimatedDueDate());
        int completed = completedGestationalWeek(canonical, serverToday);
        int completedDays = completedGestationalDays(canonical, serverToday);
        int sourceWeek = sourceWeekNumber(completed);
        return GestationalDatingResolution.noOp(
                basis,
                current.getLastMenstrualDate(),
                current.getEstimatedDueDate(),
                canonical,
                completed,
                completedDays,
                sourceWeek,
                planForSourceWeek(sourceWeek),
                false);
    }

    private GestationalDatingResolution resolveShape(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis requestedBasis,
            int contractVersion,
            GestationalDatingBasis existingBasis,
            LocalDate existingCanonicalLmp,
            LocalDate serverToday,
            boolean datingScope,
            boolean enteringPregnancy,
            boolean existingResolved) {
        validateContractVersion(contractVersion);
        if (contractVersion == V2) {
            if (requestedBasis == null
                    || (requestedBasis != GestationalDatingBasis.LMP
                    && requestedBasis != GestationalDatingBasis.EDD)
                    || (requestedBasis == GestationalDatingBasis.LMP
                    && lmp == null)
                    || (requestedBasis == GestationalDatingBasis.EDD
                    && edd == null)
                    || (requestedBasis == GestationalDatingBasis.LMP && edd != null)
                    || (requestedBasis == GestationalDatingBasis.EDD && lmp != null)) {
                throw basisRequired();
            }
        }

        if (contractVersion == V1 && existingResolved) {
            return resolveResolvedV1Correction(
                    lmp, edd, requestedBasis, existingBasis, existingCanonicalLmp,
                    serverToday);
        }

        if (lmp == null && edd == null) {
            if (contractVersion == V2) {
                throw basisRequired();
            }
            return GestationalDatingResolution.unresolved(null, null, datingScope);
        }

        if (lmp != null && edd != null) {
            if (edd.equals(lmp.plusDays(GESTATION_DAYS))) {
                return resolved(
                        GestationalDatingBasis.LMP,
                        lmp,
                        edd,
                        serverToday,
                        datingScope);
            }
            if (contractVersion == V2) {
                throw basisRequired();
            }
            // V1 preserves a non-exact legacy pair as raw, unresolved input.
            return GestationalDatingResolution.unresolved(lmp, edd, datingScope);
        }

        if (lmp != null) {
            if (contractVersion == V2 && requestedBasis != GestationalDatingBasis.LMP) {
                throw basisRequired();
            }
            return resolved(
                    GestationalDatingBasis.LMP,
                    lmp,
                    lmp.plusDays(GESTATION_DAYS),
                    serverToday,
                    datingScope);
        }

        if (contractVersion == V2 && requestedBasis != GestationalDatingBasis.EDD) {
            throw basisRequired();
        }
        return resolved(
                GestationalDatingBasis.EDD,
                null,
                edd,
                serverToday,
                datingScope);
    }

    private GestationalDatingResolution resolveResolvedV1Correction(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis requestedBasis,
            GestationalDatingBasis existingBasis,
            LocalDate existingCanonicalLmp,
            LocalDate serverToday) {
        if (requestedBasis != null && requestedBasis != existingBasis) {
            throw v2Required();
        }
        boolean authoritativeDatePresent = existingBasis == GestationalDatingBasis.LMP
                ? lmp != null
                : edd != null;
        if (!authoritativeDatePresent) {
            throw v2Required();
        }
        if (lmp != null && edd != null && !edd.equals(lmp.plusDays(GESTATION_DAYS))) {
            throw v2Required();
        }

        LocalDate candidateCanonical = existingBasis == GestationalDatingBasis.LMP
                ? lmp
                : edd.minusDays(GESTATION_DAYS);
        boolean unchanged = Objects.equals(existingCanonicalLmp, candidateCanonical);
        LocalDate sourceLmp = existingBasis == GestationalDatingBasis.LMP
                ? candidateCanonical
                : null;
        LocalDate sourceEdd = existingBasis == GestationalDatingBasis.LMP
                ? candidateCanonical.plusDays(GESTATION_DAYS)
                : edd;
        GestationalDatingResolution resolved = resolved(
                existingBasis,
                sourceLmp,
                sourceEdd,
                serverToday,
                true);
        if (unchanged) {
            return GestationalDatingResolution.noOp(
                    resolved.basis(),
                    resolved.lastMenstrualDate(),
                    resolved.estimatedDueDate(),
                    resolved.canonicalLmp(),
                    resolved.completedGestationalWeek(),
                    resolved.completedGestationalDays(),
                    resolved.sourceWeekNumber(),
                    resolved.plan());
        }
        return resolved;
    }

    private GestationalDatingResolution resolved(
            GestationalDatingBasis basis,
            LocalDate lmp,
            LocalDate edd,
            LocalDate serverToday,
            boolean datingScope) {
        LocalDate canonical = canonicalLmp(basis, lmp, edd);
        if (canonical == null) {
            throw basisRequired();
        }
        int completed = completedGestationalWeek(canonical, serverToday);
        int completedDays = completedGestationalDays(canonical, serverToday);
        int sourceWeek = sourceWeekNumber(completed);
        return new GestationalDatingResolution(
                basis,
                lmp,
                edd,
                canonical,
                true,
                false,
                datingScope,
                completed,
                completedDays,
                sourceWeek,
                planForSourceWeek(sourceWeek));
    }

    private boolean hasDating(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis basis) {
        return lmp != null || edd != null || basis != null;
    }

    private void validateContractVersion(int version) {
        if (version != V1 && version != V2) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                    "Unsupported checklist contract version");
        }
    }

    private static BusinessException basisRequired() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_BASIS_REQUIRED",
                "Pregnancy dating requires exactly one matching LMP or EDD basis");
    }

    private static BusinessException v2Required() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "GESTATIONAL_DATING_V2_REQUIRED",
                "Changing resolved pregnancy dating requires contract version 2");
    }

    private static BusinessException stageInapplicable() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_STAGE_INAPPLICABLE",
                "Gestational dating is only applicable while the Journey is pregnant");
    }

    private static BusinessException futureLmp() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_DATE_IN_FUTURE",
                "Canonical LMP cannot be in the future");
    }
}
