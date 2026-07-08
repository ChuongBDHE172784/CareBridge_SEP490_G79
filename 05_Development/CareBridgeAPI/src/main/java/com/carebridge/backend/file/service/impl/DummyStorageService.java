package com.carebridge.backend.file.service.impl;

import com.carebridge.backend.file.service.IStorageService;
import org.springframework.stereotype.Service;

@Service
public class DummyStorageService implements IStorageService {

    @Override
    public void store(String key, byte[] data, String mimeType) {
        // dummy implementation
    }

    @Override
    public String generatePresignedUrl(String key, int ttlMinutes) {
        return "http://dummy-url.com/" + key;
    }

    @Override
    public void delete(String key) {
        // dummy implementation
    }
}
