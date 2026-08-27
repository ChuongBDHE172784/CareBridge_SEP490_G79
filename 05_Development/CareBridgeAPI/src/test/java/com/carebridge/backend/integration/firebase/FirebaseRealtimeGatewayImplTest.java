package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FirebaseRealtimeGatewayImplTest {

    @Test
    void retentionPathGuard_acceptsOnlyDirectChatInboxDocuments() {
        assertThat(FirebaseRealtimeGatewayImpl.isDirectChatEventPath(
                "userConversationEvents/user-1/events/event-1")).isTrue();
        assertThat(FirebaseRealtimeGatewayImpl.isDirectChatEventPath(
                "/userConversationEvents/user-1/events/event-1")).isTrue();

        assertThat(FirebaseRealtimeGatewayImpl.isDirectChatEventPath(
                "anotherFeature/user-1/events/event-1")).isFalse();
        assertThat(FirebaseRealtimeGatewayImpl.isDirectChatEventPath(
                "userConversationEvents/user-1/other/event-1")).isFalse();
        assertThat(FirebaseRealtimeGatewayImpl.isDirectChatEventPath(
                "userConversationEvents/user-1/events/event-1/nested/value")).isFalse();
    }
}
