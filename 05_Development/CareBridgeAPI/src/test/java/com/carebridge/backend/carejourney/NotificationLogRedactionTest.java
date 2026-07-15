package com.carebridge.backend.carejourney;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** SEC-005: FCM fallback logging must not expose tokens or notification payloads. */
class NotificationLogRedactionTest {

    @Test
    void fcmStubLogsMetadataOnly() throws IOException {
        Path source = Path.of(System.getProperty("user.dir"), "src/main/java/com/carebridge/backend/notification/service/impl/FcmServiceImpl.java");
        String content = Files.readString(source);

        assertThat(content).doesNotContain("token=", "title='", "body='");
        assertThat(content).contains("tokenPresent", "titleLength", "bodyLength");
    }
}
