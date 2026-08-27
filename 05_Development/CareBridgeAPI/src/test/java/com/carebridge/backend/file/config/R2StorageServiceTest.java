package com.carebridge.backend.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.file.service.impl.R2StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class R2StorageServiceTest {

    @Test
    void generatesShortLivedPrivateUrlWithoutCallingR2() {
        R2StorageConfig config = new R2StorageConfig();
        var client = config.r2S3Client(
                "https://example.invalid", "test-access", "test-secret", "auto");
        var presigner = config.r2S3Presigner(
                "https://example.invalid", "test-access", "test-secret", "auto");
        try {
            R2StorageService service = new R2StorageService(client, presigner);
            ReflectionTestUtils.setField(service, "bucket", "carebridge-private");

            String url = service.generatePresignedUrl("files/private-id.jpg", 60);

            assertThat(url).contains("carebridge-private").contains("files/private-id.jpg");
            assertThat(service.persistedKey("files/private-id.jpg"))
                    .isEqualTo("files/private-id.jpg");
        } finally {
            presigner.close();
            client.close();
        }
    }
}
