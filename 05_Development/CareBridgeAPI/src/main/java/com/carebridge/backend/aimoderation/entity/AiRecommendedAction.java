package com.carebridge.backend.aimoderation.entity;

/**
 * Model-suggested next step. Advisory only: the server-side decision policy computes the
 * actual case outcome from classification + matched-policy severity/confidence, so a
 * prompt-injected recommendation can never drive enforcement.
 */
public enum AiRecommendedAction {
    NO_ACTION, REVIEW, PRIORITY_REVIEW, ESCALATE
}
