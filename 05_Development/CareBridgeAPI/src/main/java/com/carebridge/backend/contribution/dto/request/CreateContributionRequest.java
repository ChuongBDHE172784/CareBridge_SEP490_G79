package com.carebridge.backend.contribution.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateContributionRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    private String specialtyId;

    private String hospitalId;

    private List<AttachmentRequest> attachments;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AttachmentRequest extends BaseAttachmentRequest {
    }
}