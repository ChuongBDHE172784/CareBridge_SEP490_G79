package com.carebridge.backend.directchat.entity;

// State machine per TDS §6.3 (CB-CHAT-IMP-144D v1.2). Append-only transitions — no
// terminal state (ANSWERED/DECLINED/MISSED/CANCELLED/ENDED/FAILED) is ever revisited.
public enum CallStatus {
    INITIATED,
    RINGING,
    ANSWERED,
    DECLINED,
    MISSED,
    CANCELLED,
    ENDED,
    FAILED
}
