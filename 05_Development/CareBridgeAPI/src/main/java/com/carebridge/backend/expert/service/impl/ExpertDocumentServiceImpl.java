package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.expert.dto.response.ExpertDocumentResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import com.carebridge.backend.expert.entity.ExpertVerificationDocument;
import com.carebridge.backend.expert.entity.VerificationDocStatus;
import com.carebridge.backend.expert.entity.VerificationDocType;
import com.carebridge.backend.expert.event.DocumentUploaded;
import com.carebridge.backend.expert.mapper.IExpertDocumentMapper;
import com.carebridge.backend.expert.repository.IExpertProfileRepository;
import com.carebridge.backend.expert.repository.IExpertVerificationDocumentRepository;
import com.carebridge.backend.expert.service.IExpertDocumentService;
import com.carebridge.backend.file.service.IStorageService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ExpertDocumentServiceImpl implements IExpertDocumentService {

  private static final Logger log = LoggerFactory.getLogger(ExpertDocumentServiceImpl.class);
  private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
  private static final int MAX_DOCS = 10;
  private static final Set<String> ALLOWED_MIME = Set.of(
      "application/pdf", "image/jpeg", "image/png", "image/heic");

  private final IExpertVerificationDocumentRepository documentRepository;
  private final IExpertProfileRepository profileRepository;
  private final IStorageService storageService;
  private final IExpertDocumentMapper mapper;

  public ExpertDocumentServiceImpl(
      IExpertVerificationDocumentRepository documentRepository,
      IExpertProfileRepository profileRepository,
      IStorageService storageService,
      IExpertDocumentMapper mapper) {
    this.documentRepository = documentRepository;
    this.profileRepository = profileRepository;
    this.storageService = storageService;
    this.mapper = mapper;
  }

  @Override
  public ExpertDocumentResponse uploadDocument(
      UUID expertId, UUID accountId, MultipartFile file, VerificationDocType docType) {
    ExpertProfile profile = profileRepository.findById(expertId)
        .orElseThrow(() -> new BusinessException(
            org.springframework.http.HttpStatus.NOT_FOUND, "EXP-009",
            "Expert profile not found: " + expertId));

    if (!profile.getUserId().equals(accountId)) {
      throw new AccessDeniedBusinessException("EXP-008: Not owner of expert profile");
    }

    if (documentRepository.countByExpertId(expertId) >= MAX_DOCS) {
      throw new BusinessException(
          org.springframework.http.HttpStatus.CONFLICT, "EXP-005",
          "Max " + MAX_DOCS + " documents per profile exceeded");
    }

    if (file == null || file.isEmpty()) {
      throw new ValidationException("EXP-006: file is required");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME.contains(contentType)) {
      throw new ValidationException("EXP-006: MIME type not allowed: " + contentType);
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new ValidationException("EXP-007: File size exceeds 20MB limit");
    }

    String storageKey = UUID.randomUUID().toString();

    try {
      storageService.store(storageKey, file.getBytes(), contentType);
    } catch (Exception e) {
      throw new BusinessException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "EXP-010",
          "Failed to store file: " + e.getMessage());
    }

    ExpertVerificationDocument doc = mapper.toEntity(
        expertId, storageKey, file.getOriginalFilename(),
        contentType, file.getSize(), docType);
    doc.setId(UUID.randomUUID());

    documentRepository.save(doc);

    DocumentUploaded event = DocumentUploaded.of(doc.getId(), expertId, accountId);
    log.info("DocumentUploaded event published: documentId={}, expertId={}", doc.getId(), expertId);

    return mapper.toResponse(doc);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExpertDocumentResponse> getDocuments(UUID expertId, UUID accountId) {
    ExpertProfile profile = profileRepository.findById(expertId)
        .orElseThrow(() -> new BusinessException(
            org.springframework.http.HttpStatus.NOT_FOUND, "EXP-009",
            "Expert profile not found: " + expertId));

    if (!profile.getUserId().equals(accountId)) {
      throw new AccessDeniedBusinessException("EXP-008: Not owner of expert profile");
    }

    return documentRepository.findByExpertId(expertId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
