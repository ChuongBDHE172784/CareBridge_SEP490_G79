package com.carebridge.backend.recommendation.exception;

import org.springframework.http.HttpStatus;

/** Safe recommendation error: message contains only a field/rule, never a rejected value. */
public class RecommendationException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public RecommendationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static RecommendationException invalid(String field, String rule) {
        return new RecommendationException(HttpStatus.BAD_REQUEST, "RECOMMENDATION_PROFILE_INVALID",
                "Invalid recommendation profile field " + field + ": " + rule);
    }

    public static RecommendationException conflict(String code, String message) {
        return new RecommendationException(HttpStatus.CONFLICT, code, message);
    }

    public static RecommendationException contextUnavailable() {
        return new RecommendationException(HttpStatus.SERVICE_UNAVAILABLE,
                "RECOMMENDATION_CONTEXT_UNAVAILABLE", "Recommendation context is temporarily unavailable");
    }

    public static RecommendationException journeyRequired() {
        return new RecommendationException(HttpStatus.CONFLICT,
                "RECOMMENDATION_JOURNEY_REQUIRED", "An active maternal journey is required");
    }
}
