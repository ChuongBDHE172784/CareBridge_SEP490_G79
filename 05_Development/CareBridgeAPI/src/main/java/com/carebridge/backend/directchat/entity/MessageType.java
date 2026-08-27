package com.carebridge.backend.directchat.entity;

// Direct-chat payload discriminator. Each non-text type is validated against
// its dedicated attachment or location fields before persistence.
public enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    LOCATION
}
