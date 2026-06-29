package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ExpertDocumentResponse;
import com.carebridge.backend.expert.entity.ExpertVerificationDocument;
import java.util.UUID;

public interface IExpertDocumentMapper {

  ExpertDocumentResponse toResponse(ExpertVerificationDocument doc);

  ExpertVerificationDocument toEntity(
      UUID expertId,
      String storageKey,
      String originalName,
      String mimeType,
      Long sizeBytes,
      com.carebridge.backend.expert.entity.VerificationDocType docType);
}
