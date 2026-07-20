package com.carebridge.backend.file.service;

import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IFileService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (FILE-002/413) if > 20MB */
    UploadFileResponse uploadFile(MultipartFile file, UUID callerId);

    /**
     * Store public/community media in Cloudinary.
     * Only accepts image/* MIME types (PUBLIC content images).
     * Rejects documents (PDF/DOC/DOCX) with FILE-001.
     */
    UploadFileResponse uploadPublicFile(MultipartFile file, UUID callerId);

    /** Store sensitive evidence in private storage. Routes by MIME: images→Cloudinary(authenticated), docs→R2(private). */
    UploadFileResponse uploadPrivateFile(MultipartFile file, UUID callerId);

    /**
     * Upload with explicit routing by kind/purpose/accessMode.
     * Used by domain services that know the semantic purpose.
     * Validates detected kind matches requested kind.
     */
    UploadFileResponse uploadWithPurpose(MultipartFile file, UUID callerId,
                                          FileKind kind, FilePurpose purpose, FileAccessMode accessMode);

    /** @throws com.carebridge.backend.common.exception.ResourceNotFoundException (FILE-404) if not found/deleted */
    ViewFileResponse viewFile(UUID fileId, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.ResourceNotFoundException (FILE-404) if not found/deleted */
    void deleteFile(UUID fileId, UUID callerId);

    /** Internal compensation for a failed atomic workflow. Not exposed by FileController. */
    void purgeFile(UUID fileId, UUID callerId);
}
