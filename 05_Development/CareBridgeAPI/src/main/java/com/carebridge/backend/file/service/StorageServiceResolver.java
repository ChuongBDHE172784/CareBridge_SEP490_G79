package com.carebridge.backend.file.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import com.carebridge.backend.file.service.impl.R2StorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StorageServiceResolver {

    private final CloudinaryStorageService cloudinaryStorageService;
    private final ObjectProvider<R2StorageService> r2StorageService;

    public StorageServiceResolver(
            CloudinaryStorageService cloudinaryStorageService,
            ObjectProvider<R2StorageService> r2StorageService) {
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.r2StorageService = r2StorageService;
    }

    public IStorageService resolve(String provider) {
        if ("cloudinary".equalsIgnoreCase(provider)) {
            return cloudinaryStorageService;
        }
        if ("r2".equalsIgnoreCase(provider)) {
            R2StorageService service = r2StorageService.getIfAvailable();
            if (service != null) {
                return service;
            }
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE-005",
                    "Private R2 storage is not configured");
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-006",
                "Unknown storage provider: " + provider);
    }
}
