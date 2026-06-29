package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ExpertDocumentResponse;
import com.carebridge.backend.expert.entity.ExpertVerificationDocument;
import com.carebridge.backend.expert.entity.VerificationDocType;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ExpertDocumentMapperImpl implements IExpertDocumentMapper {

  @Override
  public ExpertDocumentResponse toResponse(ExpertVerificationDocument doc) {
    return new ExpertDocumentResponse(
        doc.getId(),
        doc.getExpertId(),
        doc.getDocType(),
        doc.getStatus(),
        doc.getStorageKey(),
        doc.getOriginalName(),
        doc.getMimeType(),
        doc.getSizeBytes(),
        doc.getUploadedAt());
  }

  @Override
  public ExpertVerificationDocument toEntity(
      UUID expertId,
      String storageKey,
      String originalName,
      String mimeType,
      Long sizeBytes,
      VerificationDocType docType) {
    return new ExpertVerificationDocument(
        expertId, docType, storageKey, originalName, mimeType, sizeBytes, null);
  }
}
