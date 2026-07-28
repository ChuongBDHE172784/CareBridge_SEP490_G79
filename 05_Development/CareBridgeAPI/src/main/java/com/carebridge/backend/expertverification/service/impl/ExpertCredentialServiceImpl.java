package com.carebridge.backend.expertverification.service.impl;

import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.dto.response.CredentialDocumentPreviewResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.mapper.ExpertCredentialMapper;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.expertverification.service.IExpertCredentialService;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.AuthorizedFileContent;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.service.IFileService;
import java.io.ByteArrayInputStream;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.exception.TikaException;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertCredentialServiceImpl implements IExpertCredentialService {

    private static final long MAX_PREVIEW_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PREVIEW_CHARS = 100_000;
    private static final java.util.Set<String> PREVIEW_MIME_TYPES = java.util.Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ExpertCredentialRepository credentialRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialMapper credentialMapper;
    private final IFileService fileService;
    private final AuditService auditService;

    @Override
    public CredentialResponse submitCredential(UUID userId, SubmitCredentialRequest request, MultipartFile file) {
        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        // Always create a new credential row — never overwrite
        if (file == null || file.isEmpty()) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPVER-006",
                    "Professional credential file is required");
        }
        LocalDate issuedDate = parseDate(request.getIssuedDate());
        LocalDate expiryDate = parseDate(request.getExpiryDate());
        UploadFileResponse uploadResponse = fileService.uploadPrivateFile(file, userId);

        var credential = credentialMapper.toEntity(
                profile.getExpertProfileId(), request, issuedDate, expiryDate, uploadResponse.getFileId());
        try {
            var saved = credentialRepository.save(credential);
            auditService.log(AuditAction.EXPERT_VERIFICATION, userId,
                    "ExpertCredential", saved.getCredentialId().toString(),
                    Map.of("event", "CREDENTIAL_SUBMITTED", "type", saved.getCredentialType()));
            return withAuthorizedUrl(credentialMapper.toResponse(saved), saved, userId);
        } catch (RuntimeException ex) {
            try {
                fileService.purgeFile(uploadResponse.getFileId(), userId);
            } catch (RuntimeException ignored) {
                // Keep the persistence error as the primary failure.
            }
            throw ex;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ExpertException(
                    HttpStatus.BAD_REQUEST, "EXPVER-005", "Invalid date format: " + value);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialResponse> getMyCredentials(UUID userId) {
        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        return credentialRepository.findProfessionalByExpertProfileId(profile.getExpertProfileId()).stream()
                .map(credential -> withAuthorizedUrl(
                        credentialMapper.toResponse(credential), credential, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialResponse getCredentialDetail(UUID credentialId, UUID userId) {
        var credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        rejectIdentityEvidence(credential);

        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (!credential.getExpertProfileId().equals(profile.getExpertProfileId())) {
            throw new ExpertException(
                    HttpStatus.FORBIDDEN, "EXPERT-005", "Insufficient permissions");
        }

        return withAuthorizedUrl(credentialMapper.toResponse(credential), credential, userId);
    }

    @Override
    public void deleteCredential(UUID credentialId, UUID userId) {
        var credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        rejectIdentityEvidence(credential);

        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (!credential.getExpertProfileId().equals(profile.getExpertProfileId())) {
            throw new ExpertException(
                    HttpStatus.FORBIDDEN, "EXPERT-005", "Insufficient permissions");
        }

        if (credential.getFileId() != null) {
            fileService.deleteFile(credential.getFileId(), userId);
        }
        credentialRepository.delete(credential);
    }

    @Override
    public DocumentReviewResponse reviewCredential(UUID credentialId, ReviewCredentialRequest request, UUID reviewerId) {
        var credential = credentialRepository.findByCredentialIdForUpdate(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        rejectIdentityEvidence(credential);

        if (request.getReviewStatus() != ReviewStatus.APPROVED
                && request.getReviewStatus() != ReviewStatus.REJECTED) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPVER-007",
                    "Review decision must be APPROVED or REJECTED");
        }
        if (request.getReviewStatus() == ReviewStatus.REJECTED
                && (request.getReviewNote() == null || request.getReviewNote().isBlank())) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPVER-008",
                    "A rejection reason is required");
        }
        if (credential.getReviewStatus() == request.getReviewStatus()) {
            var existing = credentialMapper.toDocumentReviewResponse(credential);
            applyAuthorizedUrl(existing, credential, reviewerId);
            return existing;
        }
        if (credential.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPVER-009",
                    "Credential already has a final decision");
        }

        credentialMapper.applyReview(credential, request, reviewerId);
        var saved = credentialRepository.save(credential);
        auditService.log(AuditAction.EXPERT_VERIFICATION, reviewerId,
                "ExpertCredential", saved.getCredentialId().toString(),
                Map.of("event", "CREDENTIAL_REVIEWED", "decision", saved.getReviewStatus().name()));
        var response = credentialMapper.toDocumentReviewResponse(saved);
        applyAuthorizedUrl(response, saved, reviewerId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewResponse> getPendingReviews(String credentialType, UUID reviewerId) {
        List<Object[]> rows = credentialRepository.findPendingWithExpert(ReviewStatus.PENDING);
        return rows.stream()
                .map(row -> {
                    ExpertCredential cred = (ExpertCredential) row[0];
                    var profile = (com.carebridge.backend.expert.entity.ExpertProfile) row[1];
                    var user = (com.carebridge.backend.security.entity.User) row[2];
                    var res = credentialMapper.toDocumentReviewResponse(cred);
                    applyAuthorizedUrl(res, cred, reviewerId);
                    if (profile != null) {
                        res.setExpertName(user != null ? user.getName() : null);
                        res.setSpecialty(profile.getSpecialty());
                        res.setProfessionalTitle(profile.getProfessionalTitle());
                        res.setExperienceYears(profile.getExperienceYears());
                        res.setWorkplace(profile.getWorkplace());
                        res.setPhone(user != null ? user.getPhone() : null);
                        res.setEmail(user != null ? user.getEmail() : null);
                        res.setRatingAvg(profile.getRatingAvg());
                        res.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
                    }
                    return res;
                })
                .filter(r -> credentialType == null || credentialType.isBlank() || credentialType.equals(r.getCredentialType()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialDocumentPreviewResponse previewCredential(UUID credentialId, UUID reviewerId) {
        ExpertCredential credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        rejectIdentityEvidence(credential);
        if (credential.getFileId() == null) {
            throw new ExpertException(HttpStatus.NOT_FOUND, "EXPVER-010",
                    "Credential document not found");
        }

        AuthorizedFileContent file;
        try {
            file = fileService.readAuthorizedFile(
                    credential.getFileId(), reviewerId, MAX_PREVIEW_FILE_BYTES);
        } catch (BusinessException ex) {
            if ("FILE-007".equals(ex.getCode())) {
                throw new ExpertException(HttpStatus.CONTENT_TOO_LARGE, "EXPVER-012",
                        "Document is too large to preview");
            }
            throw ex;
        }
        if (!PREVIEW_MIME_TYPES.contains(file.mimeType())) {
            throw new ExpertException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EXPVER-011",
                    "Document preview supports PDF, DOC and DOCX only");
        }
        if (file.fileSizeBytes() > MAX_PREVIEW_FILE_BYTES || file.bytes().length > MAX_PREVIEW_FILE_BYTES) {
            throw new ExpertException(HttpStatus.CONTENT_TOO_LARGE, "EXPVER-012",
                    "Document is too large to preview");
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(file.bytes())) {
            Tika tika = new Tika();
            String detectedMime = tika.detect(file.bytes(), file.originalName());
            if (!matchesDetectedMime(file.mimeType(), detectedMime)) {
                throw new ExpertException(HttpStatus.UNPROCESSABLE_ENTITY, "EXPVER-013",
                        "Document content does not match its declared type");
            }
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, file.mimeType());
            metadata.set(org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY, file.originalName());
            String extracted = tika.parseToString(input, metadata, MAX_PREVIEW_CHARS);
            String normalized = extracted
                    .replace("\u0000", "")
                    .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                    .trim();
            return CredentialDocumentPreviewResponse.builder()
                    .credentialId(credentialId)
                    .fileName(file.originalName())
                    .mimeType(file.mimeType())
                    .fileSizeBytes(file.fileSizeBytes())
                    .content(normalized)
                    .truncated(extracted.length() >= MAX_PREVIEW_CHARS)
                    .build();
        } catch (java.io.IOException | TikaException ex) {
            throw new ExpertException(HttpStatus.UNPROCESSABLE_ENTITY, "EXPVER-013",
                    "Unable to read this document");
        }
    }

    private static boolean matchesDetectedMime(String declaredMime, String detectedMime) {
        if (declaredMime == null || detectedMime == null) {
            return false;
        }
        if (declaredMime.equals(detectedMime)) {
            return true;
        }
        if ("application/msword".equals(declaredMime)) {
            return "application/x-tika-msoffice".equals(detectedMime);
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equals(declaredMime)) {
            return "application/x-tika-ooxml".equals(detectedMime)
                    || "application/zip".equals(detectedMime);
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public ViewFileResponse getCredentialFile(UUID credentialId, UUID reviewerId) {
        ExpertCredential credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        rejectIdentityEvidence(credential);
        if (credential.getFileId() == null) {
            throw new ExpertException(HttpStatus.NOT_FOUND, "EXPVER-010",
                    "Credential document not found");
        }
        return fileService.viewFile(credential.getFileId(), reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewResponse> getAdminCredentialsForProfile(
            UUID expertProfileId, UUID reviewerId) {
        return credentialRepository.findProfessionalByExpertProfileId(expertProfileId).stream()
                .map(credential -> {
                    DocumentReviewResponse response =
                            credentialMapper.toDocumentReviewResponse(credential);
                    if (credential.getFileId() != null) {
                        try {
                            ViewFileResponse metadata = fileService.getAuthorizedFileMetadata(
                                    credential.getFileId(), reviewerId);
                            response.setFileName(metadata.getOriginalName());
                            response.setMimeType(metadata.getMimeType());
                            response.setFileSizeBytes(metadata.getFileSizeBytes());
                        } catch (RuntimeException ignored) {
                            // Keep the credential visible even when its attachment is stale.
                        }
                    }
                    return response;
                })
                .toList();
    }

    private CredentialResponse withAuthorizedUrl(
            CredentialResponse response, ExpertCredential credential, UUID callerId) {
        if (credential.getFileId() != null) {
            response.setFileUrl(fileService.viewFile(credential.getFileId(), callerId).getPresignedUrl());
        }
        return response;
    }

    private static void rejectIdentityEvidence(ExpertCredential credential) {
        if (credential.getCredentialType() != null
                && credential.getCredentialType().startsWith("IDENTITY_")) {
            throw new ExpertException(
                    HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found");
        }
    }

    private void applyAuthorizedUrl(
            DocumentReviewResponse response, ExpertCredential credential, UUID callerId) {
        if (credential.getFileId() != null) {
            var file = fileService.viewFile(credential.getFileId(), callerId);
            response.setFileUrl(file.getPresignedUrl());
            response.setFileName(file.getOriginalName());
            response.setMimeType(file.getMimeType());
            response.setFileSizeBytes(file.getFileSizeBytes());
        }
    }
}
