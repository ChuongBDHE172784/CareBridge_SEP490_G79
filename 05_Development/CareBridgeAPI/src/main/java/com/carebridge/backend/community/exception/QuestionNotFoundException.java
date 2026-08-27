package com.carebridge.backend.community.exception;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(String questionId) {
        super("[COM-006] Community question not found: " + questionId);
    }
}
