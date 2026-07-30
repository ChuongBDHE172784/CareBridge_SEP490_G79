package com.carebridge.backend.file.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.service.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    // UC167: Upload file (legacy - MOTHER only)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFile(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = fileService.uploadFile(file, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }

    // UC39: Upload health record attachment (MOTHER only, images/PDF)
    @PostMapping(value = "/health-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadHealthRecordFile(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = fileService.uploadHealthRecordFile(file, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Health record file uploaded successfully"));
    }

    // UC167-EXT: Upload file with explicit purpose (EXPERT, ADMIN, etc.)
    @PostMapping(value = "/upload/with-purpose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'MODERATOR', 'CONTENT_ADMIN', 'PARTNER', 'MOTHER')")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFileWithPurpose(
            @RequestPart("file") MultipartFile file,
            @RequestParam("kind") FileKind kind,
            @RequestParam("purpose") FilePurpose purpose,
            @RequestParam("accessMode") FileAccessMode accessMode,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = fileService.uploadWithPurpose(file, callerId, kind, purpose, accessMode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }

    // UC168: View file
    @GetMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ViewFileResponse>> viewFile(
            @PathVariable UUID fileId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = fileService.viewFile(fileId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "File retrieved successfully"));
    }

    // UC169: Delete file (soft-delete)
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID fileId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        fileService.deleteFile(fileId, callerId);
        return ResponseEntity.noContent().build();
    }
}
