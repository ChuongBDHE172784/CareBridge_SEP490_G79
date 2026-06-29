package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.DocUploadTestFactory;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import com.carebridge.backend.expert.entity.ExpertVerificationDocument;
import com.carebridge.backend.expert.entity.VerificationDocStatus;
import com.carebridge.backend.expert.entity.VerificationDocType;
import com.carebridge.backend.expert.mapper.IExpertDocumentMapper;
import com.carebridge.backend.expert.repository.IExpertProfileRepository;
import com.carebridge.backend.expert.repository.IExpertVerificationDocumentRepository;
import com.carebridge.backend.expert.service.impl.ExpertDocumentServiceImpl;
import com.carebridge.backend.file.service.IStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpertDocumentServiceTest {

  private IExpertVerificationDocumentRepository documentRepository;
  private IExpertProfileRepository profileRepository;
  private IStorageService storageService;
  private IExpertDocumentMapper mapper;
  private ExpertDocumentServiceImpl service;

  @BeforeEach
  void setUp() {
    documentRepository = mock(IExpertVerificationDocumentRepository.class);
    profileRepository = mock(IExpertProfileRepository.class);
    storageService = mock(IStorageService.class);
    mapper = mock(IExpertDocumentMapper.class);
    service = new ExpertDocumentServiceImpl(documentRepository, profileRepository, storageService, mapper);
  }

  private ExpertProfile buildProfile(UUID expertId, UUID accountId) {
    return ExpertProfile.builder()
        .id(expertId)
        .userId(accountId)
        .displayName("Dr. Test")
        .status(ExpertProfileStatus.PENDING_VERIFICATION)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  // ─── DOC-TC-001: PDF upload → 201 ────────────────────────────

  @Test
  @DisplayName("DOC-TC-001: PDF upload → 201 PENDING_REVIEW")
  void uploadDocument_pdf_returnsPendingReview() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID accountId = DocUploadTestFactory.randomAccountId();
    MultipartFile file = DocUploadTestFactory.makePdfDoc();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, accountId)));
    when(documentRepository.countByExpertId(expertId)).thenReturn(0L);
    when(mapper.toEntity(any(), anyString(), anyString(), anyString(), anyLong(), any()))
        .thenAnswer(inv -> {
          ExpertVerificationDocument d = new ExpertVerificationDocument();
          d.setId(UUID.randomUUID());
          d.setExpertId(inv.getArgument(0));
          d.setStorageKey(inv.getArgument(1));
          d.setOriginalName(inv.getArgument(2));
          d.setMimeType(inv.getArgument(3));
          d.setSizeBytes(inv.getArgument(4));
          d.setDocType(inv.getArgument(5));
          d.setStatus(VerificationDocStatus.PENDING_REVIEW);
          return d;
        });
    when(documentRepository.save(any(ExpertVerificationDocument.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(ExpertVerificationDocument.class)))
        .thenAnswer(inv -> {
          ExpertVerificationDocument d = inv.getArgument(0);
          return new com.carebridge.backend.expert.dto.response.ExpertDocumentResponse(
              d.getId(), d.getExpertId(), d.getDocType(), d.getStatus(),
              d.getStorageKey(), d.getOriginalName(), d.getMimeType(),
              d.getSizeBytes(), d.getUploadedAt());
        });

    var response = service.uploadDocument(expertId, accountId, file, VerificationDocType.DEGREE);

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(VerificationDocStatus.PENDING_REVIEW);
    assertThat(response.storageKey()).isNotNull();
    verify(storageService).store(anyString(), any(byte[].class), anyString());
  }

  // ─── DOC-TC-002: File > 20MB → 400 ──────────────────────────

  @Test
  @DisplayName("DOC-TC-002: File > 20MB → 400 ValidationException")
  void uploadDocument_fileTooLarge_throwsValidation() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID accountId = DocUploadTestFactory.randomAccountId();
    MultipartFile file = DocUploadTestFactory.makeLargeFile();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, accountId)));

    assertThatThrownBy(
        () -> service.uploadDocument(expertId, accountId, file, VerificationDocType.DEGREE))
        .isInstanceOf(com.carebridge.backend.common.exception.ValidationException.class)
        .hasMessageContaining("EXP-007");
    verify(documentRepository, never()).save(any());
    verify(storageService, never()).store(anyString(), any(), anyString());
  }

  // ─── DOC-TC-003: .exe file → 400 ────────────────────────────

  @Test
  @DisplayName("DOC-TC-003: .exe file → 400 ValidationException")
  void uploadDocument_invalidMime_throwsValidation() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID accountId = DocUploadTestFactory.randomAccountId();
    MultipartFile file = DocUploadTestFactory.makeInvalidFile();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, accountId)));

    assertThatThrownBy(
        () -> service.uploadDocument(expertId, accountId, file, VerificationDocType.OTHER))
        .isInstanceOf(com.carebridge.backend.common.exception.ValidationException.class)
        .hasMessageContaining("EXP-006");
    verify(documentRepository, never()).save(any());
    verify(storageService, never()).store(anyString(), any(), anyString());
  }

  // ─── DOC-TC-004: Quota exceeded (11th doc) → 409 ────────────

  @Test
  @DisplayName("DOC-TC-004: 11th document → 409 ConflictException")
  void uploadDocument_quotaExceeded_throwsConflict() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID accountId = DocUploadTestFactory.randomAccountId();
    MultipartFile file = DocUploadTestFactory.makePdfDoc();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, accountId)));
    when(documentRepository.countByExpertId(expertId)).thenReturn(10L);

    assertThatThrownBy(
        () -> service.uploadDocument(expertId, accountId, file, VerificationDocType.DEGREE))
        .isInstanceOf(com.carebridge.backend.common.exception.BusinessException.class)
        .satisfies(ex -> {
          com.carebridge.backend.common.exception.BusinessException be =
              (com.carebridge.backend.common.exception.BusinessException) ex;
          assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(be.getCode()).isEqualTo("EXP-005");
        });
    verify(documentRepository, never()).save(any());
    verify(storageService, never()).store(anyString(), any(), anyString());
  }

  // ─── DOC-TC-005: Non-owner → 403 ────────────────────────────

  @Test
  @DisplayName("DOC-TC-005: Non-owner → 403 AccessDeniedBusinessException")
  void uploadDocument_nonOwner_throwsForbidden() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID ownerAccountId = DocUploadTestFactory.randomAccountId();
    UUID otherAccountId = UUID.randomUUID();
    MultipartFile file = DocUploadTestFactory.makePdfDoc();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, ownerAccountId)));
    when(documentRepository.countByExpertId(expertId)).thenReturn(0L);

    assertThatThrownBy(
        () -> service.uploadDocument(expertId, otherAccountId, file, VerificationDocType.DEGREE))
        .isInstanceOf(com.carebridge.backend.common.exception.AccessDeniedBusinessException.class)
        .hasMessageContaining("EXP-008");
    verify(documentRepository, never()).save(any());
    verify(storageService, never()).store(anyString(), any(), anyString());
  }

  // ─── DOC-TC-006: storageKey is UUID, not filename ────────────

  @Test
  @DisplayName("DOC-TC-006: storageKey is UUID format, not original filename")
  void uploadDocument_storageKeyIsUuid() {
    UUID expertId = DocUploadTestFactory.randomExpertId();
    UUID accountId = DocUploadTestFactory.randomAccountId();
    MultipartFile file = DocUploadTestFactory.makePdfDoc();

    when(profileRepository.findById(expertId)).thenReturn(Optional.of(buildProfile(expertId, accountId)));
    when(documentRepository.countByExpertId(expertId)).thenReturn(0L);
    when(mapper.toEntity(any(), anyString(), anyString(), anyString(), anyLong(), any()))
        .thenAnswer(inv -> {
          ExpertVerificationDocument d = new ExpertVerificationDocument();
          d.setId(UUID.randomUUID());
          d.setExpertId(inv.getArgument(0));
          d.setStorageKey(inv.getArgument(1));
          d.setOriginalName(inv.getArgument(2));
          d.setMimeType(inv.getArgument(3));
          d.setSizeBytes(inv.getArgument(4));
          d.setDocType(inv.getArgument(5));
          d.setStatus(VerificationDocStatus.PENDING_REVIEW);
          return d;
        });
    when(documentRepository.save(any(ExpertVerificationDocument.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(ExpertVerificationDocument.class)))
        .thenAnswer(inv -> {
          ExpertVerificationDocument d = inv.getArgument(0);
          return new com.carebridge.backend.expert.dto.response.ExpertDocumentResponse(
              d.getId(), d.getExpertId(), d.getDocType(), d.getStatus(),
              d.getStorageKey(), d.getOriginalName(), d.getMimeType(),
              d.getSizeBytes(), d.getUploadedAt());
        });

    var response = service.uploadDocument(expertId, accountId, file, VerificationDocType.DEGREE);

    assertThat(response.storageKey()).isNotNull();
    assertThat(response.storageKey())
        .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    assertThat(response.storageKey()).doesNotContain("degree.pdf");
  }
}
