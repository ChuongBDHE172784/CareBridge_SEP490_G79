package com.carebridge.backend.community.exception;

public class QuestionNotEditableException extends RuntimeException {

    public QuestionNotEditableException(String questionId) {
        super("[COM-010] Question cannot be edited — must be in PENDING or APPROVED status: " + questionId);
    }
}
