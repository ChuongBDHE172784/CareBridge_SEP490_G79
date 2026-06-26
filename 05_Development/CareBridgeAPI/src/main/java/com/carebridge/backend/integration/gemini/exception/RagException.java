package com.carebridge.backend.integration.gemini.exception;

import org.springframework.http.HttpStatus;

public class RagException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public RagException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static RagException invalidQuery() {
        return new RagException(
                "RAG-001",
                "Query is required and must be between 3 and 500 characters",
                HttpStatus.BAD_REQUEST);
    }

    public static RagException contextChunksExceeded() {
        return new RagException(
                "RAG-002",
                "maxContextChunks exceeds limit (max 10)",
                HttpStatus.BAD_REQUEST);
    }
}
