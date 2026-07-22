package com.carebridge.backend.contribution.dto.response;

import com.carebridge.backend.contribution.entity.ContributionStatus;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionResponse {

    private UUID id;
    private UUID expertUserId;
    private String title;
    private String content;
    private UUID specialtyId;
    private UUID hospitalId;
    private ContributionStatus status;
    private String rejectionReason;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AttachmentResponse> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResponse {
        private UUID id;
        private UUID fileId;
        private UUID contributionId;
        private FileKind kind;
        private FilePurpose purpose;
        private FileAccessMode accessMode;
        private int displayOrder;
        private String originalName;
        private String mimeType;
        private long fileSizeBytes;
        private String presignedUrl;
    }
}