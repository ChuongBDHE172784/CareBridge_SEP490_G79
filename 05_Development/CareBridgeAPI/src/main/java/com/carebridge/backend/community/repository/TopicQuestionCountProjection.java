package com.carebridge.backend.community.repository;

import java.util.UUID;

public interface TopicQuestionCountProjection {
    UUID getTopicId();
    long getCnt();
}
