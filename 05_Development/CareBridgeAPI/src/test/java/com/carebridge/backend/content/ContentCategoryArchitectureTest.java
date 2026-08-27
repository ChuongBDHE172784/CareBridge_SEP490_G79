package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class ContentCategoryArchitectureTest {
    @Test void mccTc1104_doesNotDuplicateTaxonomyEntityOrTable() throws Exception {
        Path main = Path.of("src/main/java/com/carebridge/backend/content");
        try (var files = Files.walk(main)) {
            assertFalse(files.filter(Files::isRegularFile).anyMatch(path -> path.getFileName().toString().equals("ContentCategory.java")));
        }
        try (var files = Files.walk(Path.of("src/main/resources/db/migration"))) {
            assertFalse(files.filter(Files::isRegularFile).anyMatch(path -> {
                try { return Files.readString(path).toLowerCase().contains("create table content_categories"); }
                catch (Exception error) { throw new RuntimeException(error); }
            }));
        }
    }
}
