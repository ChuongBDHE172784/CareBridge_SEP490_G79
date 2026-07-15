package com.carebridge.backend.directchat.exception;

import org.springframework.http.HttpStatus;

public class DirectChatException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public DirectChatException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static DirectChatException notMother() {
        return new DirectChatException("DCC-001", "Only a Mother may start a direct conversation", HttpStatus.FORBIDDEN);
    }

    public static DirectChatException expertNotApproved() {
        return new DirectChatException("DCC-002", "Expert is not APPROVED", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static DirectChatException expertNoLongerApproved() {
        return new DirectChatException("DCC-002", "Expert is no longer APPROVED", HttpStatus.FORBIDDEN);
    }

    public static DirectChatException notParticipant() {
        return new DirectChatException("DCC-003", "You are not a participant of this conversation", HttpStatus.FORBIDDEN);
    }

    public static DirectChatException invalidMessageBody() {
        return new DirectChatException("DCC-004", "Message body must be 1-2000 characters after trimming", HttpStatus.BAD_REQUEST);
    }

    public static DirectChatException idempotencyConflict() {
        return new DirectChatException("DCC-005", "clientMessageId already used with different content", HttpStatus.CONFLICT);
    }

    public static DirectChatException conversationNotFound() {
        return new DirectChatException("DCC-006", "Conversation not found", HttpStatus.NOT_FOUND);
    }

    public static DirectChatException expertProfileNotFound() {
        return new DirectChatException("DCC-006", "Expert profile not found", HttpStatus.NOT_FOUND);
    }

    public static DirectChatException callNotFound() {
        return new DirectChatException("DCC-006", "Call not found", HttpStatus.NOT_FOUND);
    }

    public static DirectChatException invalidCallTransition() {
        return new DirectChatException("DCC-007", "Call is not in a state that allows this transition", HttpStatus.CONFLICT);
    }

    public static DirectChatException zegoTokenFailure() {
        return new DirectChatException("DCC-008", "Failed to generate a realtime call token", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static DirectChatException wrongCallActor() {
        return new DirectChatException("DCC-009", "This action is not permitted for your role in the call", HttpStatus.FORBIDDEN);
    }

    // ADR-DCC-007 / BR-DCC-015
    public static DirectChatException expertUnavailableForWrite() {
        return new DirectChatException("DCC-010", "Expert is no longer available for this conversation", HttpStatus.CONFLICT);
    }

    public static DirectChatException invalidCursor() {
        return new DirectChatException("DCC-011", "Timeline cursor is invalid", HttpStatus.BAD_REQUEST);
    }

    public static DirectChatException conflictingCursors() {
        return new DirectChatException("DCC-011", "Use either after or before, not both", HttpStatus.BAD_REQUEST);
    }

    public static DirectChatException firebaseUnavailable() {
        return new DirectChatException("DCC-012", "Firebase realtime is not configured", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
