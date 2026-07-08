package com.carebridge.backend.file.service;

public interface IStorageService {

    /** Store raw bytes at the given key. */
    void store(String key, byte[] data, String mimeType);

    /** Generate a presigned URL valid for ttlMinutes (PDPA: max 15). */
    String generatePresignedUrl(String key, int ttlMinutes);

    /** Permanently delete object by key. */
    void delete(String key);
}
