package com.carebridge.backend.directchat.service.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.entity.MessageType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DirectMessageWriterTest {

    @Test
    void insertIfAbsent_persistsAttachmentIdForImageMessage() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        DirectMessageWriter writer = new DirectMessageWriter(jdbcTemplate);
        UUID attachmentId = UUID.randomUUID();
        DirectMessage image = DirectMessage.builder()
                .id(UUID.randomUUID()).conversationId(UUID.randomUUID()).senderUserId(UUID.randomUUID())
                .clientMessageId(UUID.randomUUID()).messageType(MessageType.IMAGE)
                .attachmentId(attachmentId).createdAt(Instant.parse("2026-08-01T00:00:00Z")).build();
        when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(1);

        writer.insertIfAbsent(image);

        verify(jdbcTemplate).update(anyString(), eq(image.getId()), eq(image.getConversationId()),
                eq(image.getSenderUserId()), eq(image.getClientMessageId()), eq("IMAGE"), eq(null),
                eq(attachmentId), org.mockito.ArgumentMatchers.any(java.sql.Timestamp.class));
    }
}
