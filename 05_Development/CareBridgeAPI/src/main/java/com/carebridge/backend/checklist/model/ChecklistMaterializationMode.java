package com.carebridge.backend.checklist.model;

/**
 * How a checklist occurrence was materialized.
 *
 * <p>{@link #CATCH_UP} rows are closed History evidence and were never
 * actionable; the remaining values describe the source of a current row.
 */
public enum ChecklistMaterializationMode {
    LEGACY,
    EVENT,
    INTERACTIVE,
    CATCH_UP
}
