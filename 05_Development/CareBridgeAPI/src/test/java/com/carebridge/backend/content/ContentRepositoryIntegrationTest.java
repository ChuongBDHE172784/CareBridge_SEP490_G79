package com.carebridge.backend.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

// TC-CLEAN-003 — see ContentImageOrphanCleanup_Test-Spec.md §4. Real DB test (not mocked): proves
// existsByBodyContaining() does NOT filter by status, so ARCHIVED content still protects its
// images from PublicContentImageCleanupJob (ADR-RTE-007 addendum, ADR-CLEAN-001).
@Transactional
class ContentRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ContentRepository contentRepository;

    @Test
    void existsByBodyContaining_archivedContent_stillCountsAsReferenced() {
        String publicId = "carebridge/orphan-cleanup-test-" + System.nanoTime();
        ContentItem archived = ContentItem.builder()
                .type(ContentType.ARTICLE)
                .title("TC-CLEAN-003 test article")
                .body("<p><img src=\"https://res.cloudinary.com/demo/image/upload/v1/" + publicId + "\"></p>")
                .status(ContentStatus.ARCHIVED)
                .build();
        contentRepository.save(archived);

        assertThat(contentRepository.existsByBodyContaining(publicId)).isTrue();
    }

    @Test
    void existsByBodyContaining_noContentReferencesIt_returnsFalse() {
        String publicId = "carebridge/definitely-not-referenced-" + System.nanoTime();

        assertThat(contentRepository.existsByBodyContaining(publicId)).isFalse();
    }
}
