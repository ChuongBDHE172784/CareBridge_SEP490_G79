package com.carebridge.backend.checklist.today.model;

/**
 * User-facing cadence classification for a task occurrence.
 *
 * <p>The Today contract intentionally exposes only the three cadence labels
 * used by the mother home screen.  Template/materialization details stay
 * server-side and are normalized to this small presentation vocabulary.</p>
 */
public enum TaskCadence {
    ONCE,
    DAILY,
    WEEKLY,
    UNKNOWN
}
