package com.carebridge.backend.community.exception;

public class CommunityTopicNotFoundException extends RuntimeException {

    public CommunityTopicNotFoundException(String topicId) {
        super("[COM-003] Community topic not found or is hidden: " + topicId);
    }
}
