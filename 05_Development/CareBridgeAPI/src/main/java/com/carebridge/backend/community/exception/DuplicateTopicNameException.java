package com.carebridge.backend.community.exception;

public class DuplicateTopicNameException extends RuntimeException {

    public DuplicateTopicNameException(String name) {
        super("Community topic with name already exists: " + name);
    }
}
