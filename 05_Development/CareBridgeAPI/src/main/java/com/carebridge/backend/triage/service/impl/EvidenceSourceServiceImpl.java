package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.entity.EvidenceSourceReviewLog;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.EvidenceSourceRepository;
import com.carebridge.backend.triage.repository.EvidenceSourceReviewLogRepository;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class EvidenceSourceServiceImpl implements EvidenceSourceService {
    private static final Set<String> BLOCKED_HOST_FRAGMENTS = Set.of(
            "facebook.", "tiktok.", "reddit.", "quora.", "youtube.", "shopee.", "lazada.");

    private final EvidenceSourceRepository evidenceSourceRepository;
    private final EvidenceSourceReviewLogRepository reviewLogRepository;

    @Override
    public EvidenceSource propose(String baseUrl, String organization, String category, String applicableStages, String notes, UUID actorUserId) {
        URI uri = parseHttpsUri(baseUrl);
        String domain = normalizeHost(uri);
        if (BLOCKED_HOST_FRAGMENTS.stream().anyMatch(domain::contains)) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "EVIDENCE-002", "Evidence source domain is not allowed");
        }
        EvidenceSource source = evidenceSourceRepository.findByDomainIgnoreCase(domain).orElse(null);
        Instant now = Instant.now();
        if (source != null && !"ARCHIVED".equals(source.getStatus())) {
            throw new TriageException(HttpStatus.CONFLICT, "EVIDENCE-003", "Evidence source domain already exists");
        }
        if (source == null) {
            source = EvidenceSource.builder()
                    .domain(domain)
                    .baseUrl("https://" + domain)
                    .createdAt(now)
                    .build();
        }
        source.setOrganization(nonBlank(organization, domain));
        source.setCategory(nonBlank(category, "OTHER"));
        source.setStatus("PENDING_REVIEW");
        source.setDiscoveryMode("MANUAL_ADMIN_ADD");
        source.setApplicableStages(nonBlank(applicableStages, "INFANT,TODDLER"));
        source.setAddedBy(actorUserId);
        source.setReviewedBy(null);
        source.setReviewedAt(null);
        source.setNotes(notes);
        source.setUpdatedAt(now);
        EvidenceSource saved = evidenceSourceRepository.save(source);
        logChange(saved.getId(), null, saved.getStatus(), notes, actorUserId, "PROPOSER");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceSource> list(String status) {
        if (status == null || status.isBlank()) {
            return evidenceSourceRepository.findAll();
        }
        return evidenceSourceRepository.findByStatusOrderByUpdatedAtDesc(status.trim().toUpperCase(Locale.ROOT));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceSource> approvedForStage(String stage) {
        String normalizedStage = stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
        return evidenceSourceRepository.findByStatus("APPROVED").stream()
                .filter(source -> java.util.Arrays.stream(source.getApplicableStages().split(","))
                        .map(String::trim)
                        .anyMatch(normalizedStage::equals))
                .toList();
    }

    @Override
    public EvidenceSource changeStatus(UUID id, String newStatus, String notes, UUID actorUserId, String actorRole) {
        EvidenceSource source = evidenceSourceRepository.findById(id)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "EVIDENCE-004", "Evidence source not found"));
        String previous = source.getStatus();
        source.setStatus(newStatus);
        source.setReviewedBy(actorUserId);
        source.setReviewedAt(Instant.now());
        source.setNotes(notes);
        source.setUpdatedAt(Instant.now());
        EvidenceSource saved = evidenceSourceRepository.save(source);
        logChange(id, previous, newStatus, notes, actorUserId, actorRole);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceSourceReviewLog> reviewLog(UUID id) {
        return reviewLogRepository.findByEvidenceSourceIdOrderByChangedAtDesc(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApprovedDeepLink(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String path = uri.getPath() == null ? "" : uri.getPath().replace("/", "").trim();
        if (path.isBlank() || Set.of("vi", "en").contains(path.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String host = normalizeHost(uri);
        return evidenceSourceRepository.findByStatus("APPROVED").stream()
                .map(EvidenceSource::getDomain)
                .filter(domain -> domain != null && !domain.isBlank())
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }

    private void logChange(UUID sourceId, String previous, String next, String notes, UUID actorUserId, String actorRole) {
        reviewLogRepository.save(EvidenceSourceReviewLog.builder()
                .evidenceSourceId(sourceId)
                .previousStatus(previous)
                .newStatus(next)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .notes(notes)
                .changedAt(Instant.now())
                .build());
    }

    private URI parseHttpsUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || "localhost".equalsIgnoreCase(uri.getHost())) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (Exception exception) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "EVIDENCE-001", "Evidence source must be a valid HTTPS URL");
        }
    }

    private String normalizeHost(URI uri) {
        return uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
