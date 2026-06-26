package com.carebridge.backend.community.exception;

public class QuestionNotAnswerableException extends RuntimeException {

    public QuestionNotAnswerableException(String questionId) {
        super("[COM-007] Question is not open for answers — must be APPROVED status: " + questionId);
    }
}
