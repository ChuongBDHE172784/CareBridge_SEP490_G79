package com.carebridge.backend.family.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-85: Pure FSM logic for CareTaskStatus.canTransitionTo().
 * No mocks needed — pure enum logic (ADR-FAM-005).
 *
 * FAM-UC85-TC-001..016: all valid, invalid, and self-transitions.
 */
class CareTaskStatusFsmTest {

    // ─── Self-transitions (always allowed — SRS E3 idempotency) ─────────────

    @ParameterizedTest(name = "self-transition {0} → {0} allowed")
    @CsvSource({"OPEN", "IN_PROGRESS", "DONE", "CANCELLED", "NEEDS_SUPPORT"})
    void selfTransition_alwaysAllowed(String status) {
        CareTaskStatus s = CareTaskStatus.valueOf(status);
        assertThat(s.canTransitionTo(s)).isTrue();
    }

    // ─── FAM-UC85-TC-001: OPEN → IN_PROGRESS (valid) ────────────────────────

    @Test
    void open_to_inProgress_allowed() {
        assertThat(CareTaskStatus.OPEN.canTransitionTo(CareTaskStatus.IN_PROGRESS)).isTrue();
    }

    // ─── FAM-UC85-TC-002: OPEN → DONE (valid, direct completion) ────────────

    @Test
    void open_to_done_allowed() {
        assertThat(CareTaskStatus.OPEN.canTransitionTo(CareTaskStatus.DONE)).isTrue();
    }

    // ─── FAM-UC85-TC-003: OPEN → NEEDS_SUPPORT (valid) ─────────────────────

    @Test
    void open_to_needsSupport_allowed() {
        assertThat(CareTaskStatus.OPEN.canTransitionTo(CareTaskStatus.NEEDS_SUPPORT)).isTrue();
    }

    // ─── FAM-UC85-TC-004: IN_PROGRESS → DONE (valid) ───────────────────────

    @Test
    void inProgress_to_done_allowed() {
        assertThat(CareTaskStatus.IN_PROGRESS.canTransitionTo(CareTaskStatus.DONE)).isTrue();
    }

    // ─── FAM-UC85-TC-005: IN_PROGRESS → NEEDS_SUPPORT (valid) ──────────────

    @Test
    void inProgress_to_needsSupport_allowed() {
        assertThat(CareTaskStatus.IN_PROGRESS.canTransitionTo(CareTaskStatus.NEEDS_SUPPORT)).isTrue();
    }

    // ─── FAM-UC85-TC-006: NEEDS_SUPPORT → IN_PROGRESS (valid, recovery) ────

    @Test
    void needsSupport_to_inProgress_allowed() {
        assertThat(CareTaskStatus.NEEDS_SUPPORT.canTransitionTo(CareTaskStatus.IN_PROGRESS)).isTrue();
    }

    // ─── FAM-UC85-TC-007: NEEDS_SUPPORT → DONE (valid) ─────────────────────

    @Test
    void needsSupport_to_done_allowed() {
        assertThat(CareTaskStatus.NEEDS_SUPPORT.canTransitionTo(CareTaskStatus.DONE)).isTrue();
    }

    // ─── FAM-UC85-TC-008: DONE → IN_PROGRESS (invalid — terminal) ───────────

    @Test
    void done_to_inProgress_blocked() {
        assertThat(CareTaskStatus.DONE.canTransitionTo(CareTaskStatus.IN_PROGRESS)).isFalse();
    }

    // ─── FAM-UC85-TC-009: DONE → OPEN (invalid — terminal) ──────────────────

    @Test
    void done_to_open_blocked() {
        assertThat(CareTaskStatus.DONE.canTransitionTo(CareTaskStatus.OPEN)).isFalse();
    }

    // ─── FAM-UC85-TC-010: DONE → NEEDS_SUPPORT (invalid — terminal) ─────────

    @Test
    void done_to_needsSupport_blocked() {
        assertThat(CareTaskStatus.DONE.canTransitionTo(CareTaskStatus.NEEDS_SUPPORT)).isFalse();
    }

    // ─── FAM-UC85-TC-011: IN_PROGRESS → OPEN (invalid — no revert) ──────────

    @Test
    void inProgress_to_open_blocked() {
        assertThat(CareTaskStatus.IN_PROGRESS.canTransitionTo(CareTaskStatus.OPEN)).isFalse();
    }

    // ─── FAM-UC85-TC-012: NEEDS_SUPPORT → OPEN (invalid) ────────────────────

    @Test
    void needsSupport_to_open_blocked() {
        assertThat(CareTaskStatus.NEEDS_SUPPORT.canTransitionTo(CareTaskStatus.OPEN)).isFalse();
    }

    // ─── CANCELLED: terminal state (same as DONE) ────────────────────────────

    @Test
    void cancelled_to_inProgress_blocked() {
        assertThat(CareTaskStatus.CANCELLED.canTransitionTo(CareTaskStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void cancelled_to_open_blocked() {
        assertThat(CareTaskStatus.CANCELLED.canTransitionTo(CareTaskStatus.OPEN)).isFalse();
    }
}
