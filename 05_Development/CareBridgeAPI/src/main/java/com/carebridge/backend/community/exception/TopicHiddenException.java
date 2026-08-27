package com.carebridge.backend.community.exception;

public class TopicHiddenException extends RuntimeException {

    public TopicHiddenException(String topicId) {
        super("[COM-014] Cannot follow a hidden topic: " + topicId);
    }
}
