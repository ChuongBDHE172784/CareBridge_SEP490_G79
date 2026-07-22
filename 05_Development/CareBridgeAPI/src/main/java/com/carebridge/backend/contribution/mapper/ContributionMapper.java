package com.carebridge.backend.contribution.mapper;

import com.carebridge.backend.contribution.dto.response.ContributionResponse;
import com.carebridge.backend.contribution.entity.ContributionAttachment;
import com.carebridge.backend.contribution.entity.MedicalContribution;
import com.carebridge.backend.file.entity.UploadedFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContributionMapper {

    ContributionMapper INSTANCE = Mappers.getMapper(ContributionMapper.class);

    @Mapping(target = "attachments", ignore = true)
    ContributionResponse toResponse(MedicalContribution contribution, List<ContributionResponse.AttachmentResponse> attachments);

    @Mapping(source = "attachment.kind", target = "kind")
    @Mapping(source = "attachment.purpose", target = "purpose")
    @Mapping(source = "attachment.accessMode", target = "accessMode")
    @Mapping(source = "attachment.displayOrder", target = "displayOrder")
    @Mapping(source = "file.originalName", target = "originalName")
    @Mapping(source = "file.mimeType", target = "mimeType")
    @Mapping(source = "file.fileSizeBytes", target = "fileSizeBytes")
    @Mapping(source = "presignedUrl", target = "presignedUrl")
    @Mapping(target = "id", source = "attachment.id")
    @Mapping(target = "fileId", source = "attachment.fileId")
    @Mapping(target = "contributionId", source = "attachment.contributionId")
    ContributionResponse.AttachmentResponse toAttachmentResponse(ContributionAttachment attachment, UploadedFile file, String presignedUrl);
}