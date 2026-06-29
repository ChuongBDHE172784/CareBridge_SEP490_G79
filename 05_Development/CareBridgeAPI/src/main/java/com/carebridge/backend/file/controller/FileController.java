package com.carebridge.backend.file.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.service.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    // UC167: Upload file
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
}
