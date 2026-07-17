package com.carebridge.backend.directchat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// MEDI-TC-009 — findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc sorts newest-first.
@Transactional
class DirectConversationRepositorySortIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private DirectConversationRepository conversationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;

    private static final UUID MOTHER_ID = UUID.randomUUID();
    private UUID convOld;
    private UUID convMid;
    private UUID convNew;

    @BeforeEach
    void seed() {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Mother Sort', ?, 'MOTHER', true, false, now(), now())",
                MOTHER_ID, "09" + String.valueOf(System.nanoTime()).substring(0, 8));

        convOld = seedConversation(Instant.parse("2026-07-10T00:00:00Z"));
        convNew = seedConversation(Instant.parse("2026-07-16T00:00:00Z"));
        convMid = seedConversation(Instant.parse("2026-07-13T00:00:00Z"));
    }

    private UUID seedConversation(Instant lastActivityAt) {
        UUID expertUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Expert Sort', ?, 'EXPERT', true, false, now(), now())",
                expertUserId, "08" + String.valueOf(System.nanoTime()).substring(0, 8));
        // chk_direct_conversations_activity_after_created requires last_activity_at >= created_at.
        jdbcTemplate.update(
                "INSERT INTO direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, ?)",
                conversationId, MOTHER_ID, expertUserId,
                java.sql.Timestamp.from(lastActivityAt), java.sql.Timestamp.from(lastActivityAt));
        return conversationId;
    }

    @Test
    void findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc_sortsNewestFirst() {
        List<DirectConversation> result = conversationRepository
                .findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID);

        assertThat(result).extracting(DirectConversation::getId)
                .containsExactly(convNew, convMid, convOld);
    }
}
