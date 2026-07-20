package com.carebridge.backend.contribution.dto.request;

import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class BaseAttachmentRequest {
    @NotNull
    private UUID fileId;

    @NotNull
    private FileKind kind;

    @NotNull
    private FilePurpose purpose;

    @NotNull
    private FileAccessMode accessMode;

    private int displayOrder = 0;
}