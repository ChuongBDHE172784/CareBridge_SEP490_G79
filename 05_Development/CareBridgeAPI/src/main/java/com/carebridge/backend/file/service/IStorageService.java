package com.carebridge.backend.file.service;

public interface IStorageService {

    /** Store raw bytes at the given key. */
    void store(String key, byte[] data, String mimeType);

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

    /** Permanently delete object by key. */
    void delete(String key);
}
