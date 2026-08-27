package com.carebridge.backend.community.exception;

public class AnswerNotFoundException extends RuntimeException {

    public AnswerNotFoundException(String answerId) {
        super("[COM-011] Community answer not found: " + answerId);
    }
}
