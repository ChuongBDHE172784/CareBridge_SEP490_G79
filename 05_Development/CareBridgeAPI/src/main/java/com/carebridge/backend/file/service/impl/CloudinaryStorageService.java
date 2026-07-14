package com.carebridge.backend.file.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.carebridge.backend.file.service.IStorageService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryStorageService implements IStorageService {

    private final Cloudinary cloudinary;
    // Thread-local holder: stores the secure_url from the most recent upload in this call-chain
    private final ThreadLocal<String> lastUrl = new ThreadLocal<>();

    public CloudinaryStorageService(
            @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
            @Value("${CLOUDINARY_API_KEY}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public void store(String key, byte[] data, String mimeType) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", "carebridge",
                    "resource_type", "auto"
            ));
            // Capture the real HTTPS URL for the caller to retrieve via generatePresignedUrl()
            lastUrl.set((String) result.get("secure_url"));
        } catch (Exception e) {
            throw new StorageException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, int ttlMinutes) {
        // Cloudinary serves files over public HTTPS — return the URL from the last upload
        String url = lastUrl.get();
        if (url != null) {
            lastUrl.remove();
            return url;
        }
        // Fallback: if no upload URL was captured, return the key as-is (shouldn't happen)
        return key;
    }

    @Override
    public void delete(String key) {
        try {
            String publicId = extractPublicId(key);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception ignored) {
            // soft-delete: don't fail the DB transaction
        }
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

    @SuppressWarnings("serial")
    public static class StorageException extends RuntimeException {
        public StorageException(String msg) { super(msg); }
        public StorageException(String msg, Throwable cause) { super(msg, cause); }
    }
}
