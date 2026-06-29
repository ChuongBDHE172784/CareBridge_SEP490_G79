package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.response.ExpertDocumentResponse;
import com.carebridge.backend.expert.entity.VerificationDocType;
import com.carebridge.backend.expert.service.IExpertDocumentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/expert-profiles")
public class ExpertDocumentController {

  private final IExpertDocumentService documentService;

  public ExpertDocumentController(IExpertDocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping(value = "/{expertId}/documents", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('EXPERT')")
  public ResponseEntity<ExpertDocumentResponse> uploadDocument(
      @PathVariable UUID expertId,
      Authentication authentication,
      @RequestParam("file") MultipartFile file,
      @RequestParam("docType") VerificationDocType docType) {
    UUID accountId = (UUID) authentication.getPrincipal();
    ExpertDocumentResponse response = documentService.uploadDocument(expertId, accountId, file, docType);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{expertId}/documents")
  @PreAuthorize("hasRole('EXPERT') or hasRole('ADMIN')")
  public ResponseEntity<List<ExpertDocumentResponse>> getDocuments(
      @PathVariable UUID expertId,
      Authentication authentication) {
    UUID accountId = (UUID) authentication.getPrincipal();
    List<ExpertDocumentResponse> documents = documentService.getDocuments(expertId, accountId);
    return ResponseEntity.ok(documents);
  }
}
