package com.carebridge.backend.content.exception;

import org.springframework.http.HttpStatus;

public class ContentException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ContentException(String code, String message, HttpStatus httpStatus) {
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

    public static ContentException duplicateContent() {
        return new ContentException(
                "CNT-002",
                "Content with same title, stage and type already exists",
                HttpStatus.CONFLICT);
    }

    public static ContentException topicNotFound(String topicId) {
        return new ContentException(
                "CNT-003",
                "Topic not found: " + topicId,
                HttpStatus.BAD_REQUEST);
    }
}
