package com.carebridge.backend.directchat.entity;

// BR-DCC-016: TEXT only in this pass — FILE/SYSTEM intentionally omitted until
// attachment security/storage/lifecycle is specified in its own TDS.
public enum MessageType {
    TEXT,
    IMAGE,
    FILE
}
