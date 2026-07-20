package com.carebridge.backend.file.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.service.IStorageService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryStorageService implements IStorageService {

    private final Cloudinary cloudinary;

    // Thread-local holder: stores the upload result from the most recent upload in this call-chain
    private final ThreadLocal<UploadResult> lastUpload = new ThreadLocal<>();

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

            String type = switch (accessMode) {
                case PUBLIC -> "upload";
                case AUTHENTICATED -> "authenticated";
                case PRIVATE -> "private";
            };

            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", "carebridge",
                    "resource_type", resourceType,
                    "type", type
            ));

            String publicId = (String) result.get("public_id");
            if (publicId == null) {
                throw new StorageException("Cloudinary response missing public_id");
            }

            lastUpload.set(new UploadResult(publicId, resourceType, accessMode));
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

    /**
     * Generate a signed URL for Cloudinary assets.
     * @param publicId The Cloudinary public ID
     * @param ttlMinutes Time to live in minutes (max 15 for PDPA)
     * @param accessMode PRIVATE, AUTHENTICATED, or PUBLIC
     * @param resourceType "image" or "raw"
     * @return Signed URL with expiration
     */
    public String generateSignedUrl(String publicId, int ttlMinutes, FileAccessMode accessMode, String resourceType) {
        int boundedTtl = Math.max(1, Math.min(ttlMinutes, 15));
        String type = switch (accessMode) {
            case PRIVATE -> "private";
            case AUTHENTICATED -> "authenticated";
            case PUBLIC -> "upload";
        };

        // Cloudinary signed URL with expiration
        // Use URL builder with query parameter for signed URL
        long expiresAt = System.currentTimeMillis() / 1000 + (boundedTtl * 60L);

        return cloudinary.url()
                .resourceType(resourceType)
                .type(type)
                .secure(true)
                .signed(true)
                .generate(publicId + "?" + "expires_at=" + expiresAt);
    }

    @Override
    public void delete(String key) {
        ParsedKey parsed = parseKey(key);
        if (parsed != null) {
            try {
                cloudinary.uploader().destroy(parsed.publicId,
                        ObjectUtils.asMap("resource_type", parsed.resourceType, "type", parsed.accessMode.name().toLowerCase()));
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
     * Delete with explicit resource type and access mode.
     * Used for proper cleanup of authenticated/private assets.
     */
    public void delete(String publicId, String resourceType, FileAccessMode accessMode) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "type", accessMode.name().toLowerCase()));
        } catch (Exception ignored) {
            // soft-delete: don't fail the DB transaction
        }
    }

    private String getResourceType(String mimeType) {
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        return "raw";
    }

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
        // Try to extract publicId from legacy key/URL
        String publicId = extractPublicId(key);
        return cloudinary.url().secure(true).generate(publicId);
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