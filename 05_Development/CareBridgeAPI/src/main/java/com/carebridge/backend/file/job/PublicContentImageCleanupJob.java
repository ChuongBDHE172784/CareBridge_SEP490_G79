package com.carebridge.backend.file.job;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges Cloudinary public content images (ADR-RTE-004/007) that were uploaded but never ended
 * up referenced by any content_items.body — e.g. the admin picked an image in the editor then
 * closed the tab, or replaced it before saving. Fulfils OI-169-3 (UC169_DeleteFile_TDS.md), scope
 * narrowed to PUBLIC_CONTENT_IMAGE + accessMode=PUBLIC only (ContentImageOrphanCleanup_TDS.md
 * ADR-CLEAN-001/002 — same {@code @Scheduled}/{@code @Value}/test-Clock pattern as
 * {@code FirebaseEventRetentionJob}).
 *
 * <p>Deliberately does NOT purge based on {@code ContentStatus} — content is never hard-deleted
 * (soft-delete/archive only), and an ARCHIVED content item still keeps its images (ADR-RTE-007
 * addendum, ContentRichTextEditor_TDS.md). "Referenced" means the image's public_id appears in
 * ANY content_items.body, regardless of status.</p>
 */
@Component
public class PublicContentImageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PublicContentImageCleanupJob.class);

    @Value("${carebridge.content.image-cleanup.enabled:true}")
    private boolean enabled;

    @Value("${carebridge.content.image-cleanup.dry-run:true}")
    private boolean dryRun;

    @Value("${carebridge.content.image-cleanup.grace-period-hours:24}")
    private long gracePeriodHours;

    private final UploadedFileRepository fileRepository;
    private final ContentRepository contentRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public PublicContentImageCleanupJob(
            UploadedFileRepository fileRepository,
            ContentRepository contentRepository,
            CloudinaryStorageService cloudinaryStorageService,
            AuditService auditService) {
        this(fileRepository, contentRepository, cloudinaryStorageService, auditService, Clock.systemUTC());
    }

    /** Test constructor. */
    PublicContentImageCleanupJob(
            UploadedFileRepository fileRepository,
            ContentRepository contentRepository,
            CloudinaryStorageService cloudinaryStorageService,
            AuditService auditService,
            Clock clock) {
        this.fileRepository = fileRepository;
        this.contentRepository = contentRepository;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedImages() {
        if (!enabled) {
            return;
        }
        Instant cutoff = Instant.now(clock).minus(Duration.ofHours(gracePeriodHours));
        List<UploadedFile> candidates = fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                FilePurpose.PUBLIC_CONTENT_IMAGE, FileAccessMode.PUBLIC, FileStatus.ACTIVE, cutoff);

        for (UploadedFile file : candidates) {
            try {
                purgeIfOrphaned(file);
            } catch (Exception ex) {
                // Best-effort hygiene job — one failure must not stop the rest of the batch or
                // escape to the scheduler (same principle as FirebaseEventRetentionJob).
                log.error("Failed to evaluate/purge candidate orphan file {}", file.getId(), ex);
            }
        }
    }

    private void purgeIfOrphaned(UploadedFile file) {
        String publicId = file.getStorageKey().split("\\|")[0];
        if (contentRepository.existsByBodyContaining(publicId)) {
            return; // still referenced by at least one content item — keep it
        }
        if (dryRun) {
            log.info("[dry-run] would purge orphaned public content image: fileId={}, publicId={}",
                    file.getId(), publicId);
            return;
        }
        cloudinaryStorageService.delete(file.getStorageKey());
        fileRepository.delete(file);
        auditService.log(AuditAction.FILE_ORPHAN_PURGED, null, "UploadedFile", file.getId().toString(),
                "orphaned public content image purged, publicId=" + publicId);
    }
}
