package com.carebridge.backend.notification.dto;

/**
 * Response for PUT /api/v1/notifications/read-all (UC-12).
 */
public record MarkAllReadResponse(int markedCount) {}
