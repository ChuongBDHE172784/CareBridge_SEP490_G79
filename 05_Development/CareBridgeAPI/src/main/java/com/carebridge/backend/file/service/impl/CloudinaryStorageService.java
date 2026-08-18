package com.carebridge.backend.file.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.service.IStorageService;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryStorageService implements IStorageService {

    private final Cloudinary cloudinary;

    // Thread-local holder: stores the upload result from the most recent upload in this call-chain
    private final ThreadLocal<UploadResult> lastUpload = new ThreadLocal<>();

    @Autowired
    public CloudinaryStorageService(
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /** Test-only seam: inject a (mock) Cloudinary client directly. */
    CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String persistedKey(String requestedKey) {
        UploadResult result = lastUpload.get();
        if (result != null) {
            lastUpload.remove();
            // Return canonical storage identifier: "publicId|resourceType|accessMode"
            return result.publicId + "|" + result.resourceType + "|" + result.accessMode.name();
        }
        return requestedKey;
    }

    @Override
    public void store(String key, byte[] data, String mimeType) {
        try {
            String resourceType = getResourceType(mimeType);
            FileAccessMode accessMode = determineAccessMode(mimeType);

            // Videos and Audios on Cloudinary must use type: "upload" (standard delivery)
            // because "private" on Cloudinary free tier blocks streaming / HTTP range requests for HTML5 <video> and <audio>
            String type = "video".equalsIgnoreCase(resourceType) || accessMode == FileAccessMode.PUBLIC
                    ? "upload"
                    : switch (accessMode) {
                        case PUBLIC -> "upload";
                        case AUTHENTICATED -> "authenticated";
                        case PRIVATE -> "private";
                    };

            String publicIdToUse = key != null ? (key.contains(".") ? key.substring(0, key.lastIndexOf('.')) : key) : null;
            java.util.Map<String, Object> uploadParams = new java.util.HashMap<>();
            uploadParams.put("resource_type", resourceType);
            uploadParams.put("type", type);
            if (publicIdToUse != null && !publicIdToUse.isBlank()) {
                uploadParams.put("public_id", publicIdToUse);
                uploadParams.put("overwrite", true);
            } else {
                uploadParams.put("folder", "carebridge");
            }

            Map<?, ?> result;
            try {
                result = cloudinary.uploader().upload(data, uploadParams);
            } catch (Exception e) {
                if (!"upload".equals(type)) {
                    uploadParams.put("type", "upload");
                    result = cloudinary.uploader().upload(data, uploadParams);
                    accessMode = FileAccessMode.PUBLIC;
                } else {
                    throw e;
                }
            }

            String publicId = (String) result.get("public_id");
            if (publicId == null) {
                throw new StorageException("Cloudinary response missing public_id");
            }

            lastUpload.set(new UploadResult(publicId, resourceType, accessMode));
        } catch (Exception e) {
            throw new StorageException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Store as a Cloudinary PUBLIC (type=upload) asset — always, regardless of MIME type.
     * Deliberately separate from {@link #store}: {@code store()} is shared by every existing
     * caller (expert identity documents, contribution attachments, generic uploads) and is left
     * byte-for-byte unchanged so this feature cannot affect those flows. Only callers that
     * explicitly want a permanent public image (content_items body images, ADR-RTE-004) use this
     * method. See ContentRichTextEditor_TDS.md ADR-RTE-007 (decoupled design).
     */
    public void storePublic(String key, byte[] data, String mimeType) {
        try {
            String resourceType = getResourceType(mimeType);
            String publicIdToUse = key != null ? (key.contains(".") ? key.substring(0, key.lastIndexOf('.')) : key) : null;
            java.util.Map<String, Object> uploadParams = new java.util.HashMap<>();
            uploadParams.put("resource_type", resourceType);
            uploadParams.put("type", "upload");
            if (publicIdToUse != null && !publicIdToUse.isBlank()) {
                uploadParams.put("public_id", publicIdToUse);
                uploadParams.put("overwrite", true);
            } else {
                uploadParams.put("folder", "carebridge");
            }

            Map<?, ?> result = cloudinary.uploader().upload(data, uploadParams);

            String publicId = (String) result.get("public_id");
            if (publicId == null) {
                throw new StorageException("Cloudinary response missing public_id");
            }

            lastUpload.set(new UploadResult(publicId, resourceType, FileAccessMode.PUBLIC));
        } catch (Exception e) {
            throw new StorageException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, int ttlMinutes) {
        // Parse canonical key: "publicId|resourceType|accessMode"
        ParsedKey parsed = parseKey(key);
        if (parsed == null) {
            // Legacy key - try to extract publicId from URL
            return fallbackGenerateUrl(key);
        }

        return generateSignedUrl(parsed.publicId, ttlMinutes, parsed.accessMode, parsed.resourceType);
    }

    @Override
    public byte[] read(String key, long maxBytes) {
        if (maxBytes < 0 || maxBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid Cloudinary read limit");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(generatePresignedUrl(key, 15)))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new StorageException(
                        "Cloudinary read failed with HTTP " + response.statusCode());
            }
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes((int) maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw new IllegalArgumentException(
                            "Stored object exceeds the allowed read size");
                }
                return bytes;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StorageException("Cloudinary read interrupted", ex);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException("Cloudinary read failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Generate a URL for Cloudinary assets.
     * @param publicId The Cloudinary public ID
     * @param ttlMinutes Time to live in minutes (max 15 for PDPA) — currently not enforced for
     *         PRIVATE/AUTHENTICATED either (see below), and ignored entirely for PUBLIC.
     * @param accessMode PRIVATE, AUTHENTICATED, or PUBLIC
     * @param resourceType "image" or "raw"
     * @return For PRIVATE/AUTHENTICATED: a Cloudinary-signed delivery URL (access requires knowing
     *         the account's api_secret to produce a valid signature — {@code s--...--} segment).
     *         For PUBLIC: a permanent, unsigned delivery URL — Cloudinary {@code type=upload}
     *         assets are public by design, so signing/expiring them serves no access-control
     *         purpose and breaks any use case that persists the URL (e.g. images embedded in rich
     *         text content — ContentRichTextEditor_TDS.md ADR-RTE-004).
     *
     *         <p><b>Fixed bug (was: HTTP 400 on every PRIVATE/AUTHENTICATED request):</b> this used
     *         to build the URL by concatenating {@code "?expires_at=" + timestamp} onto the
     *         public_id string; Cloudinary parsed the whole thing as the public_id (not a query
     *         param) and rejected every such request with HTTP 400 "public_id ... is invalid" —
     *         confirmed live against a real Cloudinary account, both before and after this fix.
     *         This affected all PRIVATE/AUTHENTICATED delivery, including expert identity
     *         documents and contribution attachments.</p>
     *
     *         <p><b>Known limitation (not this bug, pre-existing, out of scope):</b> the returned
     *         URL does not actually enforce a {@code ttlMinutes} expiry — Cloudinary only supports
     *         real time-boxed URL expiry via its account-level "Token-based Authentication"
     *         feature (a separate signing key configured in the Cloudinary dashboard, not present
     *         in this project's config today). Access is still restricted to holders of a validly
     *         signed URL, just not time-limited. Enabling true expiry is a follow-up.</p>
     */
    public String generateSignedUrl(String publicId, int ttlMinutes, FileAccessMode accessMode, String resourceType) {
        if (accessMode == FileAccessMode.PUBLIC || "video".equalsIgnoreCase(resourceType)) {
            return cloudinary.url()
                    .resourceType(resourceType != null ? resourceType : "image")
                    .type("upload")
                    .secure(true)
                    .generate(publicId);
        }

        String type = switch (accessMode) {
            case PRIVATE -> "private";
            case AUTHENTICATED -> "authenticated";
            case PUBLIC -> throw new IllegalStateException("handled above");
        };

        return cloudinary.url()
                .resourceType(resourceType != null ? resourceType : "image")
                .type(type)
                .secure(true)
                .signed(true)
                .generate(publicId);
    }

    @Override
    public void delete(String key) {
        ParsedKey parsed = parseKey(key);
        if (parsed != null) {
            try {
                cloudinary.uploader().destroy(parsed.publicId,
                        ObjectUtils.asMap("resource_type", parsed.resourceType, "type", cloudinaryDeleteType(parsed.accessMode)));
            } catch (Exception ignored) {
                // soft-delete: don't fail the DB transaction
            }
            return;
        }
        // Legacy fallback - try to extract from URL
        try {
            String publicId = extractPublicId(key);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception ignored) {
            // soft-delete: don't fail the DB transaction
        }
    }

    /**
     * Required deletion path for domain-owned community media. Unlike the legacy best-effort
     * delete method, this propagates failures so the community soft-delete transaction can abort.
     */
    public void deleteRequired(String key) {
        try {
            ParsedKey parsed = parseKey(key);
            if (parsed != null) {
                cloudinary.uploader().destroy(parsed.publicId,
                        ObjectUtils.asMap("resource_type", parsed.resourceType,
                                "type", cloudinaryDeleteType(parsed.accessMode)));
                return;
            }
            String publicId = extractPublicId(key);
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image", "type", "upload"));
        } catch (Exception ex) {
            throw new StorageException("Cloudinary delete failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete with explicit resource type and access mode.
     * Used for proper cleanup of authenticated/private assets.
     */
    public void delete(String publicId, String resourceType, FileAccessMode accessMode) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "type", cloudinaryDeleteType(accessMode)));
        } catch (Exception ignored) {
            // soft-delete: don't fail the DB transaction
        }
    }

    /**
     * Maps accessMode to the Cloudinary `type` param expected by the destroy API. Only PUBLIC
     * differs from {@code accessMode.name().toLowerCase()} ("upload" vs "public") — PRIVATE/
     * AUTHENTICATED deletes are byte-identical to the original mapping, so this is safe to use
     * unconditionally without affecting the existing (non-public) delete flows. Needed because
     * {@link #storePublic} is the first caller that can actually produce a PUBLIC-accessMode key.
     */
    private static String cloudinaryDeleteType(FileAccessMode accessMode) {
        return accessMode == FileAccessMode.PUBLIC ? "upload" : accessMode.name().toLowerCase();
    }

    private String getResourceType(String mimeType) {
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        return "raw";
    }

    /**
     * Determine access mode from the purpose/policy, not just MIME type.
     * This should be overridden by the policy-based approach in FileServiceImpl.
     * For backward compatibility, default based on MIME.
     */
    private FileAccessMode determineAccessMode(String mimeType) {
        // Images uploaded via store() default to AUTHENTICATED (private delivery)
        // Public images should use uploadPublicFile() which forces PUBLIC mode
        if (mimeType.startsWith("image/")) return FileAccessMode.AUTHENTICATED;
        return FileAccessMode.PRIVATE;
    }

    private ParsedKey parseKey(String key) {
        if (key == null || !key.contains("|")) return null;
        String[] parts = key.split("\\|", 3);
        if (parts.length != 3) return null;
        try {
            return new ParsedKey(parts[0], parts[1], FileAccessMode.valueOf(parts[2]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String fallbackGenerateUrl(String key) {
        String resourceType = "image";
        if (key != null) {
            String lower = key.toLowerCase();
            if (lower.endsWith(".webm") || lower.endsWith(".mp4") || lower.endsWith(".mov")
                    || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3")
                    || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".weba")) {
                resourceType = "video";
            } else if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
                    || lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
                resourceType = "raw";
            }
        }
        String publicId = extractPublicId(key);
        return cloudinary.url()
                .resourceType(resourceType)
                .type("upload")
                .secure(true)
                .generate(publicId);
    }

    private String extractPublicId(String url) {
        // URL format: https://res.cloudinary.com/<cloud>/image/upload/v1234567890/carebridge/xxx.ext
        try {
            String uploadMarker = "/upload/";
            int idx = url.indexOf(uploadMarker);
            if (idx >= 0) {
                String afterUpload = url.substring(idx + uploadMarker.length());
                // Strip version prefix like "v1234567890/"
                int slash = afterUpload.indexOf('/');
                if (slash >= 0 && afterUpload.substring(0, slash).matches("v\\d+")) {
                    afterUpload = afterUpload.substring(slash + 1);
                }
                int queryIndex = afterUpload.indexOf('?');
                if (queryIndex >= 0) afterUpload = afterUpload.substring(0, queryIndex);
                int fragmentIndex = afterUpload.indexOf('#');
                if (fragmentIndex >= 0) afterUpload = afterUpload.substring(0, fragmentIndex);
                int lastSlash = afterUpload.lastIndexOf('/');
                int extensionIndex = afterUpload.lastIndexOf('.');
                if (extensionIndex > lastSlash) {
                    afterUpload = afterUpload.substring(0, extensionIndex);
                }
                return afterUpload;
            }
        } catch (Exception ignored) {}
        return url;
    }

    private static class UploadResult {
        final String publicId;
        final String resourceType;
        final FileAccessMode accessMode;

        UploadResult(String publicId, String resourceType, FileAccessMode accessMode) {
            this.publicId = publicId;
            this.resourceType = resourceType;
            this.accessMode = accessMode;
        }
    }

    private static class ParsedKey {
        final String publicId;
        final String resourceType;
        final FileAccessMode accessMode;

        ParsedKey(String publicId, String resourceType, FileAccessMode accessMode) {
            this.publicId = publicId;
            this.resourceType = resourceType;
            this.accessMode = accessMode;
        }
    }

    @SuppressWarnings("serial")
    public static class StorageException extends RuntimeException {
        public StorageException(String msg) { super(msg); }
        public StorageException(String msg, Throwable cause) { super(msg, cause); }
    }
}
