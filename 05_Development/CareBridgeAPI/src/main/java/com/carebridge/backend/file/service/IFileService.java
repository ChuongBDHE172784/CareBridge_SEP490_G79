package com.carebridge.backend.file.service;

import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IFileService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (FILE-002/413) if > 20MB */
    UploadFileResponse uploadFile(MultipartFile file, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.ResourceNotFoundException (FILE-404) if not found/deleted */
    ViewFileResponse viewFile(UUID fileId, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.ResourceNotFoundException (FILE-404) if not found/deleted */
    void deleteFile(UUID fileId, UUID callerId);
}
