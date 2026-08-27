package com.carebridge.backend.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.file.entity.UploadedFile;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UploadedFileStorageProviderPersistenceTest {

    @Test
    void storageProviderIsMappedToTheAttachmentsTable() throws Exception {
        var field = UploadedFile.class.getDeclaredField("storageProvider");
        var column = field.getAnnotation(Column.class);

        assertThat(field.getAnnotation(Transient.class)).isNull();
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("storage_provider");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void migrationBackfillsR2MediaAndKeepsCloudinaryKeys() throws Exception {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V6__persist_attachment_storage_provider.sql")) {
            assertThat(stream).isNotNull();
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration)
                .contains("ADD COLUMN IF NOT EXISTS \"storage_provider\"")
                .contains("\"storage_key\" LIKE '%|%'")
                .contains("\"mime_type\" LIKE 'image/%'")
                .contains("ELSE 'r2'")
                .contains("ALTER COLUMN \"storage_provider\" SET NOT NULL")
                .contains("attachments_storage_provider_ck");
    }
}
