package com.carebridge.backend.file.service;

public interface IStorageService {

    /** Store raw bytes at the given key. */
    void store(String key, byte[] data, String mimeType);

    /**
     * Store raw bytes as a permanent PUBLIC asset (Cloudinary type=upload). Deliberately separate
     * from {@link #store} so this narrow use case (public content images, ADR-RTE-004/007) cannot
     * change behavior for any existing caller of {@link #store}. Not supported by every provider —
     * default throws; only {@code CloudinaryStorageService} overrides it.
     */
    default void storePublic(String key, byte[] data, String mimeType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support storePublic()");
    }

    /**
     * Return the immutable value that must be persisted after {@link #store}.
     * Private object stores keep the requested key. Legacy providers may return
     * their provider URL for backward compatibility.
     */
    default String persistedKey(String requestedKey) {
        return requestedKey;
    }

    /** Generate a presigned URL valid for ttlMinutes (PDPA: max 15). */
    String generatePresignedUrl(String key, int ttlMinutes);

    /** Read private object bytes after the domain service has authorized the caller. */
    default byte[] read(String key) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support read()");
    }

    default byte[] read(String key, long maxBytes) {
        byte[] bytes = read(key);
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("Stored object exceeds the allowed read size");
        }
        return bytes;
    }

    /** Permanently delete object by key. */
    void delete(String key);
}
