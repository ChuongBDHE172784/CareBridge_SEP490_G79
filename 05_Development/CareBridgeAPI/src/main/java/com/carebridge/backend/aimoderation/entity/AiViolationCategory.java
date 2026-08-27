package com.carebridge.backend.aimoderation.entity;

/**
 * Content-moderation violation taxonomy. It is broader than the user-facing
 * {@code ReportCategory}; each policy maps back to a ReportCategory for the moderation
 * case's reason_code. Emergency triage remains an independent runtime safety concern.
 */
public enum AiViolationCategory {
    SPAM_ADVERTISING,
    HARASSMENT_BULLYING,
    HATE_SPEECH,
    CHILD_SAFETY,
    SELF_HARM_ENCOURAGEMENT,
    DANGEROUS_MEDICAL_ADVICE,
    EXPERT_IMPERSONATION,
    HARMFUL_MISINFORMATION,
    PII_DOXXING,
    SCAM_FRAUD,
    PROMPT_INJECTION,
    OTHER
}
