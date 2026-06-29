package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.UploadDocumentRequest;
import com.carebridge.backend.expert.dto.response.ExpertDocumentResponse;
import com.carebridge.backend.expert.entity.VerificationDocType;
import java.util.UUID;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IExpertDocumentService {

  ExpertDocumentResponse uploadDocument(
      UUID expertId, UUID accountId, MultipartFile file, VerificationDocType docType);

  List<ExpertDocumentResponse> getDocuments(UUID expertId, UUID accountId);
}
