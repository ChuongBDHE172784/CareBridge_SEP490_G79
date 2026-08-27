package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.UnpublishRequest;
import com.carebridge.backend.content.entity.*;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentUnpublishServiceImpl;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnpublishContentServiceImplTest {
    static final UUID ADMIN = UUID.fromString("f1a00000-0000-0000-0000-0000000000ad");
    static final UUID CONTENT = UUID.fromString("f1b00000-0000-0000-0000-000000000001");
    static final Instant PUBLISHED = Instant.parse("2026-05-01T08:00:00Z");
    @Mock ContentRepository repository;
    @Mock AuditService audit;
    @InjectMocks ContentUnpublishServiceImpl service;

    ContentItem item(ContentStatus status) {
        return ContentItem.builder().id(CONTENT).status(status).publishedAt(PUBLISHED).build();
    }

    @Test void upcTc1201_archivesApprovedAndPreservesPublishedAt() {
        ContentItem target = item(ContentStatus.APPROVED);
        when(repository.findById(CONTENT)).thenReturn(Optional.of(target));
        when(repository.save(target)).thenReturn(target);
        var response = service.unpublish(CONTENT, new UnpublishRequest("outdated"), ADMIN);
        assertEquals(ContentStatus.ARCHIVED, target.getStatus());
        assertEquals(ContentStatus.APPROVED, response.previousStatus());
        assertEquals(ContentStatus.ARCHIVED, response.newStatus());
        assertEquals(PUBLISHED, target.getPublishedAt()); assertEquals(PUBLISHED, response.publishedAt());
    }

    static Stream<ContentStatus> invalidStatuses() {
        return Stream.of(ContentStatus.DRAFT, ContentStatus.PENDING_REVIEW, ContentStatus.ARCHIVED);
    }

    @ParameterizedTest @MethodSource("invalidStatuses")
    void upcTc1202_rejectsNonApproved(ContentStatus status) {
        when(repository.findById(CONTENT)).thenReturn(Optional.of(item(status)));
        ContentException error = assertThrows(ContentException.class,
                () -> service.unpublish(CONTENT, new UnpublishRequest("outdated"), ADMIN));
        assertEquals("CNT-010", error.getCode()); verify(repository, never()).save(any());
    }

    @Test void upcTc1204_doesNotInventUnpublishedStatus() {
        assertEquals(Set.of("DRAFT", "PENDING_REVIEW", "APPROVED", "ARCHIVED"),
                Arrays.stream(ContentStatus.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void upcTc1205_requiresReason() {
        ContentException error = assertThrows(ContentException.class,
                () -> service.unpublish(CONTENT, new UnpublishRequest(" "), ADMIN));
        assertEquals("CNT-011", error.getCode()); verifyNoInteractions(repository);
    }

    @Test void upcTc1206_notFound() {
        when(repository.findById(CONTENT)).thenReturn(Optional.empty());
        ContentException error = assertThrows(ContentException.class,
                () -> service.unpublish(CONTENT, new UnpublishRequest("outdated"), ADMIN));
        assertEquals("CNT-003", error.getCode());
    }

    @Test void upcTc1207_auditsOnce() {
        ContentItem target = item(ContentStatus.APPROVED);
        when(repository.findById(CONTENT)).thenReturn(Optional.of(target));
        when(repository.save(target)).thenReturn(target);
        service.unpublish(CONTENT, new UnpublishRequest("outdated"), ADMIN);
        verify(audit).log(AuditAction.CONTENT_UNPUBLISHED, ADMIN, "CONTENT_ITEM", CONTENT.toString(), "reason=outdated");
    }
}
