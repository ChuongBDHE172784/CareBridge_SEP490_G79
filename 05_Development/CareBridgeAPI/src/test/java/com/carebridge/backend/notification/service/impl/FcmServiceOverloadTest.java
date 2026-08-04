package com.carebridge.backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.firebase.messaging.Message;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FcmServiceOverloadTest {

    @Test
    void disabledStubAcceptsDataOverloadWithoutBreakingLegacyMethods() {
        FcmServiceImpl service = new FcmServiceImpl();

        assertThat(service.isReady()).isFalse();
        assertThat(service.sendWithRetry(
                        "token",
                        "Title",
                        "Body",
                        Map.of("type", "MESSAGE", "conversationId", "conversation-id"),
                        1)
                .success())
                .isFalse();
        assertThat(service.sendToToken("token", "Title", "Body")).isNull();
        assertThat(service.sendToTokens(java.util.List.of("token"), "Title", "Body")).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void firebaseImplementationCopiesAllDataIntoTheMessage() throws Exception {
        Map<String, String> data =
                Map.of("type", "MESSAGE", "conversationId", "conversation-id");

        Message message =
                FirebaseFcmServiceImpl.buildDataMessage("token", "Title", "Body", data);

        Method getData = Message.class.getDeclaredMethod("getData");
        getData.setAccessible(true);
        assertThat((Map<String, String>) getData.invoke(message)).isEqualTo(data);
    }
}
