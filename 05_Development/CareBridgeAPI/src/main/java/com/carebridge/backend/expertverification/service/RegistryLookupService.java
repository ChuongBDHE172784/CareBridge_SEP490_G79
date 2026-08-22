package com.carebridge.backend.expertverification.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expertverification.dto.response.RegistryLookupResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.registry.HcmMedinetRegistrySource;
import com.carebridge.backend.expertverification.registry.LicenseNumberNormalizer;
import com.carebridge.backend.expertverification.registry.LicenseNumberNormalizer.NormalizedLicense;
import com.carebridge.backend.expertverification.registry.RegistryHtmlCache;
import com.carebridge.backend.expertverification.registry.RegistryMatcher;
import com.carebridge.backend.expertverification.registry.RegistryQueryResult;
import com.carebridge.backend.expertverification.registry.RegistryRow;
import com.carebridge.backend.expertverification.registry.RegistrySource;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Checks one declared practice licence against the public HCM registry, on admin demand
 * (MF-05 Spec 05).
 *
 * <p>Three rules shape this class and none of them are negotiable:
 * <ul>
 *   <li>The licence number is read from the stored credential, never from the request, so this can
 *       never become a free-text lookup service for arbitrary numbers (§4.2).</li>
 *   <li>A miss is never evidence of fraud. The dataset only covers people practising in HCMC, so a
 *       legitimate doctor registered elsewhere simply is not in it (§3.2).</li>
 *   <li>Nothing here changes {@code reviewStatus}. The admin still decides (§8.5).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistryLookupService {

    private static final String MEDICAL_LICENSE = "MEDICAL_LICENSE";
    private static final double AUTO_CONFIDENCE_FLOOR = 0.85;

    private static final String ADVISORY_NOT_FOUND =
            "Không tìm thấy trong dữ liệu Sở Y tế TP.HCM. Nguồn này chỉ chứa người đang hành nghề "
                    + "tại TP.HCM — không tìm thấy KHÔNG có nghĩa là giấy phép không hợp lệ. "
                    + "Vui lòng đối chiếu ảnh tài liệu.";
    private static final String ADVISORY_SOURCE_ERROR =
            "Không kết nối được cổng tra cứu Sở Y tế tại thời điểm này. Vui lòng thử lại sau hoặc "
                    + "đối chiếu thủ công.";
    private static final String ADVISORY_DISABLED =
            "Tính năng đối chiếu với Sở Y tế đang tạm tắt. Vui lòng đối chiếu thủ công.";
    private static final String ADVISORY_NOT_APPLICABLE =
            "Chỉ đối chiếu được giấy phép hành nghề y có số chứng chỉ. Loại giấy tờ này không có "
                    + "nguồn tra cứu công khai.";
    private static final String ADVISORY_UNREADABLE_NUMBER =
            "Số chứng chỉ chuyên gia khai không đúng định dạng (ví dụ đúng: 001563/HCM-CCHN) nên "
                    + "không thể đối chiếu tự động. Vui lòng kiểm tra ảnh tài liệu.";
    private static final String ADVISORY_RED_FLAG =
            "Số giấy phép này có tồn tại nhưng đứng tên người khác trong dữ liệu Sở Y tế TP.HCM. "
                    + "Cần kiểm tra kỹ trước khi duyệt.";
    private static final String ADVISORY_MASKED_MATCH =
            "Khớp một phần: bản ghi nguồn bị che một phần số chứng chỉ. Cần đối chiếu thêm ảnh tài liệu.";
    private static final String SOURCE_NOTE =
            "Nguồn không hỗ trợ liên kết trực tiếp tới từng hồ sơ. Nút 'Xem trang nguồn' mở lại "
                    + "đúng trang kết quả đã nhận tại thời điểm tra cứu.";

    private final ExpertCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final RegistrySource registrySource;
    private final RegistryHtmlCache htmlCache;
    private final AuditService auditService;

    /**
     * Runs, or replays, one lookup for a credential.
     *
     * <p>Not {@code @Transactional}: the outbound call can take up to 15 seconds and holding a
     * database connection open for that would starve the pool under a handful of concurrent
     * reviews.
     */
    public RegistryLookupResponse lookup(UUID credentialId, UUID adminId, boolean force) {
        ExpertCredential credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));

        if (!MEDICAL_LICENSE.equals(credential.getCredentialType())
                || credential.getCredentialNumber() == null
                || credential.getCredentialNumber().isBlank()) {
            return terminal(credentialId, credential.getCredentialNumber(), null,
                    "NOT_APPLICABLE", ADVISORY_NOT_APPLICABLE);
        }

        if (!force) {
            Optional<RegistryHtmlCache.Entry> cached = htmlCache.get(credentialId);
            if (cached.isPresent() && cached.get().response() != null) {
                RegistryLookupResponse replay = cached.get().response();
                replay.setFromCache(true);
                return replay;
            }
        }

        String declaredNumber = credential.getCredentialNumber().trim();
        Optional<NormalizedLicense> normalized = LicenseNumberNormalizer.normalize(declaredNumber);
        if (normalized.isEmpty()) {
            // Still worth telling the admin, but there is nothing sensible to send to the source.
            return terminal(credentialId, declaredNumber, null,
                    "NOT_FOUND", ADVISORY_UNREADABLE_NUMBER);
        }

        String declaredName = resolveExpertName(credential);
        RegistryQueryResult queryResult = query(declaredNumber, normalized.get());
        Instant queriedAt = Instant.now();

        RegistryLookupResponse response = switch (queryResult.status()) {
            case DISABLED -> terminal(credentialId, declaredNumber, normalized.get(),
                    "DISABLED", ADVISORY_DISABLED);
            case SOURCE_ERROR -> terminal(credentialId, declaredNumber, normalized.get(),
                    "SOURCE_ERROR", ADVISORY_SOURCE_ERROR);
            case OK -> evaluate(credentialId, declaredNumber, normalized.get(), declaredName,
                    queryResult, queriedAt);
        };
        response.setQueriedAt(queriedAt);

        if (queryResult.status() == RegistryQueryResult.Status.OK) {
            htmlCache.put(credentialId, queryResult.pageHtml(), response, queriedAt);
        }
        writeAudit(credentialId, adminId, declaredNumber, normalized.get(), response,
                queryResult, queriedAt);
        return response;
    }

    /**
     * Sends the declared string first, because the portal matches it as typed. Only if that finds
     * nothing is the bare serial tried, which catches suffix typos and masked source records
     * (§5, §3.5). Two requests per click at most.
     */
    private RegistryQueryResult query(String declaredNumber, NormalizedLicense normalized) {
        RegistryQueryResult direct = registrySource.lookup(declaredNumber);
        if (direct.status() != RegistryQueryResult.Status.OK || !direct.rows().isEmpty()) {
            return direct;
        }
        RegistryQueryResult bySerial = registrySource.lookup(normalized.serial());
        return bySerial.status() == RegistryQueryResult.Status.OK ? bySerial : direct;
    }

    private RegistryLookupResponse evaluate(
            UUID credentialId,
            String declaredNumber,
            NormalizedLicense normalized,
            String declaredName,
            RegistryQueryResult queryResult,
            Instant queriedAt) {

        RegistryMatcher.MatchOutcome outcome =
                RegistryMatcher.match(queryResult.rows(), normalized, declaredName);

        RegistryLookupResponse.RegistryLookupResponseBuilder builder = baseBuilder(
                credentialId, declaredNumber, normalized)
                .result(outcome.result().name())
                .confidence(outcome.confidence())
                .redFlag(outcome.redFlag());

        RegistryRow row = outcome.matchedRow();
        if (row != null) {
            builder.matched(RegistryLookupResponse.MatchedRecord.builder()
                    .fullName(row.fullName())
                    .licenseNo(row.licenseNo())
                    .practiceScope(row.practiceScope())
                    .statusText(row.statusText())
                    .sourceRecordId(row.sourceRecordId())
                    .build());
            builder.nameComparison(RegistryLookupResponse.NameComparison.builder()
                    .declared(declaredName)
                    .registry(row.fullName())
                    .similarity(outcome.nameSimilarity())
                    .consistent(!outcome.redFlag())
                    .build());
        }

        builder.advisory(advisoryFor(outcome));
        builder.source(sourceInfo(credentialId, outcome.result() != RegistryMatcher.MatchResult.NOT_FOUND,
                queriedAt));
        return builder.build();
    }

    private String advisoryFor(RegistryMatcher.MatchOutcome outcome) {
        return switch (outcome.result()) {
            case NOT_FOUND -> ADVISORY_NOT_FOUND;
            case FUZZY -> outcome.redFlag() ? ADVISORY_RED_FLAG : ADVISORY_MASKED_MATCH;
            case MATCHED -> null;
        };
    }

    private RegistryLookupResponse terminal(
            UUID credentialId, String declaredNumber, NormalizedLicense normalized,
            String result, String advisory) {
        return baseBuilder(credentialId, declaredNumber, normalized)
                .result(result)
                .confidence(0.0)
                .advisory(advisory)
                .queriedAt(Instant.now())
                .build();
    }

    private RegistryLookupResponse.RegistryLookupResponseBuilder baseBuilder(
            UUID credentialId, String declaredNumber, NormalizedLicense normalized) {
        return RegistryLookupResponse.builder()
                .credentialId(credentialId)
                .declaredLicenseNo(declaredNumber)
                .normalizedLicenseNo(normalized == null ? null : normalized.canonical())
                .fromCache(false)
                .source(RegistryLookupResponse.SourceInfo.builder()
                        .name(registrySource.displayName())
                        .note(SOURCE_NOTE)
                        .build());
    }

    private RegistryLookupResponse.SourceInfo sourceInfo(
            UUID credentialId, boolean hasViewablePage, Instant queriedAt) {
        RegistryLookupResponse.SourceInfo.SourceInfoBuilder builder =
                RegistryLookupResponse.SourceInfo.builder()
                        .name(registrySource.displayName())
                        .note(SOURCE_NOTE);
        if (hasViewablePage) {
            builder.sourceViewUrl("/api/v1/expert/credentials/" + credentialId
                            + "/registry-lookup/source")
                    .sourceViewExpiresAt(queriedAt.plus(htmlCache.ttl()));
        }
        return builder.build();
    }

    /** Serves the stored result page. Empty when the short-lived cache has already dropped it. */
    public Optional<RegistryHtmlCache.Entry> sourcePage(UUID credentialId) {
        credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));
        return htmlCache.get(credentialId);
    }

    private String resolveExpertName(ExpertCredential credential) {
        return userRepository.findById(credential.getExpertProfileId())
                .map(user -> user.getName())
                .orElse("");
    }

    /**
     * One row per click. {@code audit_events} rejects updates and deletes, so the history is the
     * audit trail — no bespoke versioning needed (§2.3).
     */
    private void writeAudit(
            UUID credentialId,
            UUID adminId,
            String declaredNumber,
            NormalizedLicense normalized,
            RegistryLookupResponse response,
            RegistryQueryResult queryResult,
            Instant queriedAt) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "REGISTRY_LOOKUP");
        payload.put("source", HcmMedinetRegistrySource.SOURCE_CODE);
        payload.put("queriedAt", queriedAt.toString());
        payload.put("declaredLicenseNo", declaredNumber);
        payload.put("normalizedLicenseNo", normalized == null ? null : normalized.canonical());
        payload.put("result", response.getResult());
        payload.put("confidence", response.getConfidence());
        payload.put("rowCount", queryResult.rows().size());
        payload.put("autoVerifiable",
                "MATCHED".equals(response.getResult())
                        && response.getConfidence() >= AUTO_CONFIDENCE_FLOOR);

        if (response.getMatched() != null) {
            Map<String, Object> matched = new HashMap<>();
            matched.put("fullName", response.getMatched().getFullName());
            matched.put("licenseNo", response.getMatched().getLicenseNo());
            matched.put("practiceScope", response.getMatched().getPracticeScope());
            matched.put("statusText", response.getMatched().getStatusText());
            matched.put("sourceRecordId", response.getMatched().getSourceRecordId());
            payload.put("matched", matched);
            payload.put("evidenceHtml", evidenceHtml(queryResult.rows(), response));
        }

        try {
            auditService.log(AuditAction.EXPERT_VERIFICATION, adminId,
                    "ExpertCredential", credentialId.toString(), payload);
        } catch (RuntimeException auditFailure) {
            // The admin has a usable answer on screen; losing the trail must not lose the answer.
            log.warn("Registry lookup audit write failed credentialId={} reason={}",
                    credentialId, auditFailure.getClass().getSimpleName());
        }
    }

    /**
     * Only the matched row is retained. The full page can list unrelated people and Decree
     * 13/2023/NĐ-CP gives us no reason to keep their data (§9).
     */
    private String evidenceHtml(List<RegistryRow> rows, RegistryLookupResponse response) {
        String licenseNo = response.getMatched().getLicenseNo();
        return rows.stream()
                .filter(row -> row.licenseNo().equals(licenseNo))
                .findFirst()
                .map(row -> truncate(row.rowHtml()))
                .orElse(null);
    }

    private static String truncate(String html) {
        int limit = 32 * 1024;
        if (html == null || html.length() <= limit) {
            return html;
        }
        return html.substring(0, limit);
    }
}
