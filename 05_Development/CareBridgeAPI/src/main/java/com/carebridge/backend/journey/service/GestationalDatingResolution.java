package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.entity.GestationalDatingBasis;

import java.time.LocalDate;

/**
 * Immutable result of resolving the server-owned pregnancy dating contract.
 *
 * <p>The source dates are kept separate from {@link #canonicalLmp()}: an EDD
 * based journey deliberately retains a null source LMP while the resolver
 * still exposes the derived canonical anchor used by cadence.</p>
 */
public record GestationalDatingResolution(
        GestationalDatingBasis basis,
        LocalDate lastMenstrualDate,
        LocalDate estimatedDueDate,
        LocalDate canonicalLmp,
        boolean resolved,
        boolean semanticNoOp,
        boolean datingScope,
        int completedGestationalWeek,
        int completedGestationalDays,
        int sourceWeekNumber,
        Integer plan) {

    public static GestationalDatingResolution unresolved(
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            boolean datingScope) {
        return new GestationalDatingResolution(
                null,
                lastMenstrualDate,
                estimatedDueDate,
                null,
                false,
                false,
                datingScope,
                -1,
                -1,
                -1,
                null);
    }

    public static GestationalDatingResolution noOp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            LocalDate canonicalLmp,
            int completedGestationalWeek,
            int completedGestationalDays,
            int sourceWeekNumber,
            Integer plan) {
        return noOp(basis, lastMenstrualDate, estimatedDueDate, canonicalLmp,
                completedGestationalWeek, completedGestationalDays, sourceWeekNumber, plan, true);
    }

    public static GestationalDatingResolution noOp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            LocalDate canonicalLmp,
            int completedGestationalWeek,
            int completedGestationalDays,
            int sourceWeekNumber,
            Integer plan,
            boolean datingScope) {
        return new GestationalDatingResolution(
                basis,
                lastMenstrualDate,
                estimatedDueDate,
                canonicalLmp,
                true,
                true,
                datingScope,
                completedGestationalWeek,
                completedGestationalDays,
                sourceWeekNumber,
                plan);
    }
}
