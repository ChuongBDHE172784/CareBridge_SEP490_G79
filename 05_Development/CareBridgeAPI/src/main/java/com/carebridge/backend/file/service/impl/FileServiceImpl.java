package com.carebridge.backend.file.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.dto.AuthorizedFileContent;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.policy.FileAccessPolicy;
import com.carebridge.backend.file.policy.FileDeletePolicy;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.file.service.IStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileServiceImpl implements IFileService {

    private static final long MAX_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_FILES_PER_ACCOUNT = 500L;

    // IMAGE MIME types → Cloudinary
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/gif"
    );

    // MEDIA MIME types (Video / Audio recordings) → R2
    private static final Set<String> MEDIA_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime",
            "audio/mp4", "audio/x-m4a", "audio/m4a", "audio/aac", "audio/mpeg", "audio/wav", "audio/ogg", "audio/webm"
    );

    // DOCUMENT MIME types → R2
    private static final String MIME_DOC = "application/msword";
    private static final String MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String MIME_XLS = "application/vnd.ms-excel";
    private static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final Set<String> DOCUMENT_MIME_TYPES = java.util.stream.Stream.concat(
            Set.of("application/pdf", MIME_DOC, MIME_DOCX, MIME_XLS, MIME_XLSX).stream(),
            MEDIA_MIME_TYPES.stream()
    ).collect(Collectors.toUnmodifiableSet());

    private static final Set<String> HEALTH_RECORD_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/gif",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = java.util.stream.Stream.of(
            IMAGE_MIME_TYPES,
            DOCUMENT_MIME_TYPES
    ).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    private final UploadedFileRepository fileRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ObjectProvider<R2StorageService> r2StorageService;
    private final String configuredProvider;
    private final AuditService auditService;
    private final FileAccessPolicy fileAccessPolicy;
    private final FileDeletePolicy fileDeletePolicy;

    public FileServiceImpl(
            UploadedFileRepository fileRepository,
            CloudinaryStorageService cloudinaryStorageService,
            ObjectProvider<R2StorageService> r2StorageService,
            AuditService auditService,
            FileAccessPolicy fileAccessPolicy,
            FileDeletePolicy fileDeletePolicy,
            @Value("${carebridge.storage.provider:cloudinary}") String configuredProvider) {
        this.fileRepository = fileRepository;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.r2StorageService = r2StorageService;
        this.auditService = auditService;
        this.fileAccessPolicy = fileAccessPolicy;
        this.fileDeletePolicy = fileDeletePolicy;
        this.configuredProvider = configuredProvider;
    }

    @Override
    public UploadFileResponse uploadFile(MultipartFile file, UUID callerId) {
        // Default behavior: route based on MIME type
        String mimeType = detectMimeType(file);
        String provider = determineProvider(mimeType);
        return uploadUsing(file, callerId, provider, null, null, null);
    }

    @Override
    public UploadFileResponse uploadPublicFile(MultipartFile file, UUID callerId) {
        String mimeType = detectMimeType(file);
        // Public files must be images only - reject documents
        if (!IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Public file upload only supports image types (JPEG, PNG, WebP, HEIC, GIF). Got: " + mimeType);
        }
        // Public files are always Cloudinary with PUBLIC access mode
        return uploadUsing(file, callerId, "cloudinary", FileKind.IMAGE, FilePurpose.PUBLIC_CONTENT_IMAGE, FileAccessMode.PUBLIC);
    }

    @Override
    public UploadFileResponse uploadPrivateFile(MultipartFile file, UUID callerId) {
        String mimeType = detectMimeType(file);
        // Private files route by MIME type
        return uploadUsing(file, callerId, determineProvider(mimeType), null, null, FileAccessMode.PRIVATE);
    }

    @Override
    public UploadFileResponse uploadHealthRecordFile(MultipartFile file, UUID callerId) {
        String mimeType = detectMimeType(file);
        if (!HEALTH_RECORD_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Health record file upload only supports images and PDF files. Got: " + mimeType);
        }

        FileKind kind = IMAGE_MIME_TYPES.contains(mimeType) ? FileKind.IMAGE : FileKind.DOCUMENT;
        FilePurpose purpose = kind == FileKind.IMAGE
                ? FilePurpose.MEDICAL_CONTRIBUTION_IMAGE
                : FilePurpose.MEDICAL_CONTRIBUTION_DOCUMENT;

        return uploadUsing(file, callerId, determineProvider(mimeType), kind, purpose, FileAccessMode.PRIVATE);
    }

    /**
     * Upload with explicit routing by kind/purpose/accessMode.
     * Used by domain services that know the semantic purpose.
     * Validates that detected kind matches requested kind.
     */
    @Override
    public UploadFileResponse uploadWithPurpose(MultipartFile file, UUID callerId,
                                                 FileKind kind, FilePurpose purpose, FileAccessMode accessMode) {
        String mimeType = detectMimeType(file);
        String provider = determineProviderByKind(kind);

        // Validate: detected kind must match requested kind
        FileKind detectedKind = IMAGE_MIME_TYPES.contains(mimeType) ? FileKind.IMAGE : FileKind.DOCUMENT;
        if (detectedKind != kind) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    String.format("File kind mismatch: declared %s but detected %s (MIME: %s)", kind, detectedKind, mimeType));
        }

        return uploadUsing(file, callerId, provider, kind, purpose, accessMode);
    }

    @Override
    public FileKind detectKind(MultipartFile file) {
        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Unsupported file type: " + mimeType);
        }
        return IMAGE_MIME_TYPES.contains(mimeType) ? FileKind.IMAGE : FileKind.DOCUMENT;
    }

    private String determineProvider(String mimeType) {
        if (IMAGE_MIME_TYPES.contains(mimeType)) {
            return "cloudinary";
        }
        if (DOCUMENT_MIME_TYPES.contains(mimeType)) {
            R2StorageService r2 = r2StorageService.getIfAvailable();
            if (r2 != null) return "r2";
            // Fail closed - don't fall back to Cloudinary for documents
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE-005",
                    "Private R2 storage is not configured for documents");
        }
        // Unknown MIME - default to cloudinary for backward compat, but log warning
        return "cloudinary";
    }

    private String determineProviderByKind(FileKind kind) {
        if (kind == FileKind.IMAGE) return "cloudinary";
        if (kind == FileKind.DOCUMENT) {
            R2StorageService r2 = r2StorageService.getIfAvailable();
            if (r2 != null) return "r2";
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE-005",
                    "Private R2 storage is not configured for documents");
        }
        return "cloudinary";
    }

    private UploadFileResponse uploadUsing(MultipartFile file, UUID callerId, String provider,
                                           FileKind kind, FilePurpose purpose, FileAccessMode accessMode) {
        // C5: size check (FILE-002)
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE-002",
                    "File exceeds maximum size of 20MB");
        }

        // C4: storage quota check BEFORE writing to storage
        long existingCount = fileRepository.countByOwnerUserIdAndStatus(callerId, FileStatus.ACTIVE);
        if (existingCount >= MAX_FILES_PER_ACCOUNT) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-003",
                    "Storage quota of 500 files exceeded");
        }

        // C1: validate MIME from magic bytes (not extension)
        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Unsupported file type: " + mimeType);
        }

        // Determine kind if not provided
        if (kind == null) {
            kind = IMAGE_MIME_TYPES.contains(mimeType) ? FileKind.IMAGE : FileKind.DOCUMENT;
        }

        // Determine purpose/accessMode if not provided
        if (purpose == null) {
            // Infer purpose from provider + kind
            if (provider.equals("cloudinary") && kind == FileKind.IMAGE) {
                purpose = FilePurpose.PUBLIC_CONTENT_IMAGE;
                accessMode = FileAccessMode.AUTHENTICATED;
            } else if (provider.equals("r2") && kind == FileKind.DOCUMENT) {
                purpose = FilePurpose.MEDICAL_CONTRIBUTION_DOCUMENT;
                accessMode = FileAccessMode.PRIVATE;
            } else {
                purpose = FilePurpose.PUBLIC_CONTENT_IMAGE;
                accessMode = FileAccessMode.PRIVATE;
            }
        }
        if (accessMode == null) {
            accessMode = provider.equals("cloudinary") ? FileAccessMode.AUTHENTICATED : FileAccessMode.PRIVATE;
        }

        // C2: storageKey must be UUID-based (not originalName)
        String fileExt = getExtension(file.getOriginalFilename(), mimeType);
        String storageKey = "files/" + UUID.randomUUID() + fileExt;

        // Calculate checksum for integrity (single read - bytes cached from detectMimeType)
        String checksum = null;
        try {
            checksum = calculateChecksum(file.getBytes());
        } catch (IOException ignored) {
            // Continue without checksum
        }

        IStorageService storageService = storageFor(provider);
        String persistedStorageKey;
        try {
            // PUBLIC images use a dedicated storePublic() path so this can never change behavior
            // for the shared store() used by expert identity / contribution / generic uploads
            // (ADR-RTE-007, decoupled design — ContentRichTextEditor_TDS.md).
            if (accessMode == FileAccessMode.PUBLIC) {
                storageService.storePublic(storageKey, file.getBytes(), mimeType);
            } else {
                storageService.store(storageKey, file.getBytes(), mimeType);
            }
            persistedStorageKey = storageService.persistedKey(storageKey);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-004",
                    "Failed to store file: " + e.getMessage());
        }

        try {
            UploadedFile saved = fileRepository.save(UploadedFile.builder()
                    .ownerUserId(callerId)
                    .uploaderRole(resolveCallerRole())
                    .storageKey(persistedStorageKey)
                    .storageProvider(provider)
                    .kind(kind)
                    .purpose(purpose)
                    .accessMode(accessMode)
                    .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                    .mimeType(mimeType)
                    .fileSizeBytes(file.getSize())
                    .checksum(checksum)
                    .status(FileStatus.ACTIVE)
                    .build());
            String presignedUrl = storageService.generatePresignedUrl(saved.getStorageKey(), 15);
            if (accessMode == FileAccessMode.PUBLIC
                    && (purpose == FilePurpose.COMMUNITY_QUESTION_IMAGE
                        || purpose == FilePurpose.COMMUNITY_ANSWER_IMAGE)) {
                saved.setFileUrl(presignedUrl);
                fileRepository.save(saved);
            }
            auditService.log(AuditAction.FILE_UPLOADED, callerId,
                    "UploadedFile", saved.getId().toString(), "uploaded");
            return UploadFileResponse.builder()
                    .fileId(saved.getId())
                    .originalName(saved.getOriginalName())
                    .mimeType(saved.getMimeType())
                    .fileSizeBytes(saved.getFileSizeBytes())
                    .presignedUrl(presignedUrl)
                    .createdAt(saved.getCreatedAt())
                    .build();
        } catch (RuntimeException ex) {
            try {
                storageService.delete(persistedStorageKey);
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    private String detectMimeType(MultipartFile file) {
        try {
            byte[] header = file.getBytes();
            if (header.length >= 2) {
                if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8) return "image/jpeg";
                if (header.length >= 4
                        && header[0] == 0x25 && header[1] == 0x50
                        && header[2] == 0x44 && header[3] == 0x46) return "application/pdf";
                if (header.length >= 8
                        && header[0] == (byte) 0x89 && header[1] == 0x50
                        && header[2] == 0x4E && header[3] == 0x47) return "image/png";
                if (header.length >= 12
                        && header[0] == 0x52 && header[1] == 0x49
                        && header[2] == 0x46 && header[3] == 0x46
                        && header[8] == 0x57 && header[9] == 0x45
                        && header[10] == 0x42 && header[11] == 0x50) return "image/webp";
                if (header.length >= 6
                        && header[0] == 0x47 && header[1] == 0x49
                        && header[2] == 0x46) return "image/gif";
                // OLE Compound File: D0 CF 11 E0 A1 B1 1A E1. Both .doc and .xls start
                // this way, so the header alone cannot tell them apart - an Excel workbook
                // used to be stored labelled as Word. Read the stream names in the OLE
                // directory instead; a workbook always carries a "Workbook" stream.
                if (header.length >= 8
                        && header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF
                        && header[2] == 0x11 && header[3] == (byte) 0xE0
                        && header[4] == (byte) 0xA1 && header[5] == (byte) 0xB1
                        && header[6] == 0x1A && header[7] == (byte) 0xE1) {
                    return looksLikeOleWorkbook(header) ? MIME_XLS : MIME_DOC;
                }
                // OOXML (ZIP-based): PK 03 04. .docx and .xlsx are identical here too;
                // the part names inside the archive are what separate them.
                if (header.length >= 4
                        && header[0] == 0x50 && header[1] == 0x4B
                        && (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07)
                        && header[3] == 0x04) {
                    String ooxml = detectOoxmlSubtype(header);
                    if (ooxml != null) return ooxml;
                    return MIME_DOCX;
                }
            }
            String declared = file.getContentType();
            return declared != null ? declared : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    /**
     * Reads the part names out of an OOXML package. Word puts its body under word/,
     * Excel under xl/ - that is the only reliable difference between a .docx and an
     * .xlsx, which are both plain ZIP archives with identical magic bytes.
     */
    private String detectOoxmlSubtype(byte[] bytes) {
        try (java.util.zip.ZipInputStream zip =
                     new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            int inspected = 0;
            while ((entry = zip.getNextEntry()) != null && inspected++ < 64) {
                String name = entry.getName();
                if (name.startsWith("xl/")) return MIME_XLSX;
                if (name.startsWith("word/")) return MIME_DOCX;
            }
        } catch (IOException | IllegalArgumentException e) {
            // Not a readable archive - fall back to the caller's other checks.
        }
        return null;
    }

    /**
     * An OLE compound file keeps its stream names as UTF-16LE inside the directory.
     * A .xls always has a stream called "Workbook" (or "Book" on very old files);
     * a .doc has "WordDocument". Scanning the raw bytes for those names avoids
     * pulling in a full OLE parser for a single distinction.
     */
    private boolean looksLikeOleWorkbook(byte[] bytes) {
        return containsUtf16(bytes, "Workbook") || containsUtf16(bytes, "Book");
    }

    private boolean containsUtf16(byte[] haystack, String needle) {
        byte[] pattern = needle.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        outer:
        for (int i = 0; i + pattern.length <= haystack.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (haystack[i + j] != pattern[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public ViewFileResponse viewFile(UUID fileId, UUID callerId) {
        UploadedFile file = requireViewableFile(fileId, callerId);

        String presignedUrl = storageFor(resolveProvider(file))
                .generatePresignedUrl(file.getStorageKey(), 15);

        auditService.log(AuditAction.FILE_VIEWED, callerId,
                "UploadedFile", file.getId().toString(), "viewed");

        return ViewFileResponse.builder()
                .fileId(file.getId())
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .fileSizeBytes(file.getFileSizeBytes())
                .presignedUrl(presignedUrl)
                .status(file.getStatus().name())
                .createdAt(file.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizedFileContent readAuthorizedFile(UUID fileId, UUID callerId, long maxBytes) {
        UploadedFile file = requireViewableFile(fileId, callerId);
        if (file.getFileSizeBytes() > maxBytes) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE-007",
                    "File is too large to preview");
        }
        byte[] bytes;
        try {
            bytes = storageFor(resolveProvider(file)).read(file.getStorageKey(), maxBytes);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE-007",
                    "File is too large to preview");
        }
        auditService.log(AuditAction.FILE_VIEWED, callerId,
                "UploadedFile", file.getId().toString(), "previewed");
        return new AuthorizedFileContent(
                file.getId(), file.getOriginalName(), file.getMimeType(),
                file.getFileSizeBytes(), bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public ViewFileResponse getAuthorizedFileMetadata(UUID fileId, UUID callerId) {
        UploadedFile file = requireViewableFile(fileId, callerId);
        return ViewFileResponse.builder()
                .fileId(file.getId())
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .fileSizeBytes(file.getFileSizeBytes())
                .status(file.getStatus().name())
                .createdAt(file.getCreatedAt())
                .build();
    }

    private UploadedFile requireViewableFile(UUID fileId, UUID callerId) {
        UploadedFile file = fileRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        Set<String> authorities = java.util.Optional
                .ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        fileAccessPolicy.assertViewable(file, callerId, authorities);
        return file;
    }

    private String resolveProvider(UploadedFile file) {
        if (DOCUMENT_MIME_TYPES.contains(file.getMimeType())) {
            return "r2";
        }
        return file.getStorageProvider();
    }

    @Override
    public void deleteFile(UUID fileId, UUID callerId) {
        UploadedFile file = fileRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        fileDeletePolicy.assertDeletable(file, callerId);

        // Soft-delete only — DO NOT call storageService.delete() (ADR-FILE-008)
        file.setStatus(FileStatus.DELETED);
        fileRepository.save(file);

        auditService.log(AuditAction.FILE_DELETED, callerId,
                "UploadedFile", file.getId().toString(), "deleted");
    }

    @Override
    public void purgeFile(UUID fileId, UUID callerId) {
        fileRepository.findById(fileId).ifPresent(file -> {
            if (!file.getOwnerUserId().equals(callerId)) {
                throw new AccessDeniedBusinessException("Access denied to file " + fileId);
            }
            storageFor(file.getStorageProvider()).delete(file.getStorageKey());
            fileRepository.delete(file);
        });
    }

    @Override
    public void purgeCommunityImages(List<String> imageUrls, UUID ownerUserId) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        Set<String> urls = imageUrls.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(url -> url.startsWith("https://res.cloudinary.com/"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (urls.isEmpty()) {
            return;
        }

        List<UploadedFile> trackedFiles = fileRepository
                .findAllByOwnerUserIdAndFileUrlInAndStatus(ownerUserId, urls, FileStatus.ACTIVE);
        Set<String> trackedUrls = trackedFiles.stream()
                .map(UploadedFile::getFileUrl)
                .collect(Collectors.toSet());
        if (!trackedUrls.equals(urls)) {
            throw new AccessDeniedBusinessException(
                    "Community image ownership could not be verified");
        }

        for (UploadedFile file : trackedFiles) {
            cloudinaryStorageService.deleteRequired(file.getStorageKey());
            file.setStatus(FileStatus.DELETED);
        }
        if (!trackedFiles.isEmpty()) {
            fileRepository.saveAll(trackedFiles);
        }

    }

    @Override
    public void assertCommunityImagesOwned(List<String> imageUrls, UUID ownerUserId) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        Set<String> urls = imageUrls.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(url -> url.startsWith("https://res.cloudinary.com/"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<UploadedFile> trackedFiles = fileRepository
                .findAllByOwnerUserIdAndFileUrlInAndStatus(ownerUserId, urls, FileStatus.ACTIVE);
        Set<String> trackedUrls = trackedFiles.stream()
                .map(UploadedFile::getFileUrl)
                .collect(Collectors.toSet());
        if (!trackedUrls.equals(urls)) {
            throw new AccessDeniedBusinessException(
                    "Community image ownership could not be verified");
        }
    }

    @Override
    public String generatePresignedUrl(UUID fileId, UUID callerId, int ttlMinutes) {
        UploadedFile file = fileRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Set<String> authorities = java.util.Optional
                .ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        fileAccessPolicy.assertViewable(file, callerId, authorities);

        return storageFor(file.getStorageProvider()).generatePresignedUrl(file.getStorageKey(), ttlMinutes);
    }

    @Override
    public UploadFileResponse uploadPrivateBytes(byte[] bytes, UUID callerId, String mimeType, String suggestedName) {
        return uploadPrivateBytes(bytes, callerId, mimeType, suggestedName, null);
    }

    @Override
    public UploadFileResponse uploadPrivateBytes(byte[] bytes, UUID callerId, String mimeType, String suggestedName, FilePurpose purpose) {
        // Size check (FILE-002)
        if (bytes.length > MAX_SIZE_BYTES) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE-002",
                    "File exceeds maximum size of 20MB");
        }

        // Storage quota check (C4)
        long existingCount = fileRepository.countByOwnerUserIdAndStatus(callerId, FileStatus.ACTIVE);
        if (existingCount >= MAX_FILES_PER_ACCOUNT) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-003",
                    "Storage quota of 500 files exceeded");
        }

        // Validate MIME type (C1)
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Unsupported file type: " + mimeType);
        }

        // Determine kind
        FileKind kind = IMAGE_MIME_TYPES.contains(mimeType) ? FileKind.IMAGE : FileKind.DOCUMENT;

        // For cropped face images, use Cloudinary with PRIVATE access
        String provider = kind == FileKind.IMAGE ? "cloudinary" : "r2";
        FilePurpose effectivePurpose = purpose != null ? purpose :
            (kind == FileKind.IMAGE ? FilePurpose.EXPERT_IDENTITY_SELFIE_CROP : FilePurpose.EXPERT_IDENTITY_CCCD_FRONT_CROP);
        FileAccessMode accessMode = FileAccessMode.PRIVATE;

        // Generate storage key
        String fileExt = getExtension(suggestedName, mimeType);
        String storageKey = "files/" + UUID.randomUUID() + fileExt;

        // Calculate checksum
        String checksum = null;
        try {
            checksum = calculateChecksum(bytes);
        } catch (Exception ignored) {
            // Continue without checksum
        }

        IStorageService storageService = storageFor(provider);
        String persistedStorageKey;
        try {
            storageService.store(storageKey, bytes, mimeType);
            persistedStorageKey = storageService.persistedKey(storageKey);
        } catch (RuntimeException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-004",
                    "Failed to store file: " + e.getMessage());
        }

        try {
            UploadedFile saved = fileRepository.save(UploadedFile.builder()
                    .ownerUserId(callerId)
                    .uploaderRole(resolveCallerRole())
                    .storageKey(persistedStorageKey)
                    .storageProvider(provider)
                    .kind(kind)
                    .purpose(effectivePurpose)
                    .accessMode(accessMode)
                    .originalName(suggestedName != null ? suggestedName : "crop-" + System.currentTimeMillis() + fileExt)
                    .mimeType(mimeType)
                    .fileSizeBytes((long) bytes.length)
                    .checksum(checksum)
                    .status(FileStatus.ACTIVE)
                    .build());
            String presignedUrl = storageService.generatePresignedUrl(saved.getStorageKey(), 15);
            auditService.log(AuditAction.FILE_UPLOADED, callerId,
                    "UploadedFile", saved.getId().toString(), "uploaded");
            return UploadFileResponse.builder()
                    .fileId(saved.getId())
                    .originalName(saved.getOriginalName())
                    .mimeType(saved.getMimeType())
                    .fileSizeBytes(saved.getFileSizeBytes())
                    .presignedUrl(presignedUrl)
                    .createdAt(saved.getCreatedAt())
                    .build();
        } catch (RuntimeException ex) {
            try {
                storageService.delete(persistedStorageKey);
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    private IStorageService storageFor(String provider) {
        if ("cloudinary".equalsIgnoreCase(provider)) {
            return cloudinaryStorageService;
        }
        if ("r2".equalsIgnoreCase(provider)) {
            R2StorageService service = r2StorageService.getIfAvailable();
            if (service != null) return service;
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE-005",
                    "Private R2 storage is not configured");
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-006",
                "Unknown storage provider: " + provider);
    }

    private String getExtension(String filename, String mimeType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        // Fallback by mime type
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            case "audio/mp4", "audio/x-m4a", "audio/m4a" -> ".m4a";
            case "audio/aac" -> ".aac";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "audio/webm" -> ".weba";
            default -> "";
        };
    }

    private String resolveCallerRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            for (var authority : auth.getAuthorities()) {
                String role = authority.getAuthority();
                if (role.startsWith("ROLE_")) {
                    return role.substring(5);
                }
                return role;
            }
        }
        return "PATIENT";
    }
}
