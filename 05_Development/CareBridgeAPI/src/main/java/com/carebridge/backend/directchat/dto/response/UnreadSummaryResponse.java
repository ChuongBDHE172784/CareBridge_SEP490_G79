package com.carebridge.backend.directchat.dto.response;

public record UnreadSummaryResponse(int unreadConversationCount, int totalUnreadMessageCount) {
}
