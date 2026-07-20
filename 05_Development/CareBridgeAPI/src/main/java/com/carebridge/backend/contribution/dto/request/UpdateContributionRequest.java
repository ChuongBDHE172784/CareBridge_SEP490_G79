package com.carebridge.backend.contribution.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateContributionRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String content;

    private String specialtyId;

    private String hospitalId;

    private List<AttachmentRequest> attachments;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AttachmentRequest extends BaseAttachmentRequest {
    }
}