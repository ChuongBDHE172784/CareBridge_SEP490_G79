package com.carebridge.backend.aimoderation.entity;

/**
 * Durable scan-job lifecycle. FAILED is terminal and observable — it is never interpreted
 * as SAFE. SKIPPED covers target-gone / stale-content / no-active-policy outcomes.
 */
public enum AiScanJobStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED, SKIPPED
}
