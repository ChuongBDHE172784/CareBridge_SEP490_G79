package com.carebridge.backend.community.exception;

public class AnswerNotEditableException extends RuntimeException {

    public AnswerNotEditableException(String answerId) {
        super("[COM-013] Answer cannot be edited — must not be HIDDEN or DELETED: " + answerId);
    }
}
