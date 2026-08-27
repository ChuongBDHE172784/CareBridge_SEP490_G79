package com.carebridge.backend.community.exception;

public class TopicHasDependentsException extends RuntimeException {

    public TopicHasDependentsException(String message) {
        super(message);
    }
}
