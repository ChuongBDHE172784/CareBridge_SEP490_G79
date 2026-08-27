package com.carebridge.backend.content.service;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentWorkloadDispatcherServiceImpl implements ContentWorkloadDispatcherService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ContentRepository contentRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    @Override
    @Transactional(readOnly = true)
    public UUID dispatchToOptimalExpert(ContentType type, ContentStage stage, UUID preferredExpertId) {
        List<ExpertProfile> contractedExperts = expertProfileRepository.findActiveContractedExperts();

        if (contractedExperts.isEmpty()) {
            log.warn("No active CONTRACTED experts available for content dispatch (type={}, stage={})", type, stage);
            return null;
        }

        // 1. Nếu có preferredExpertId (ví dụ người từng review trước khi reject) và chuyên gia này vẫn active & contracted
        if (preferredExpertId != null) {
            boolean isPreferredStillValid = contractedExperts.stream()
                    .anyMatch(ep -> ep.getUserId().equals(preferredExpertId));
            if (isPreferredStillValid) {
                log.info("Dispatching content to preferred previous reviewer: {}", preferredExpertId);
                return preferredExpertId;
            }
        }

        // 2. Khớp chuyên môn (Specialty Affinity - soft filter)
        List<ExpertProfile> candidatePool = filterBySpecialty(contractedExperts, stage);
        if (candidatePool.isEmpty()) {
            candidatePool = contractedExperts;
        }

        // 3. Tính toán Active Workload (số bài đang PENDING_REVIEW) cho từng candidate
        Set<UUID> candidateIds = candidatePool.stream()
                .map(ExpertProfile::getUserId)
                .collect(Collectors.toSet());

        Map<UUID, Long> workloadMap = computeActiveWorkload(candidateIds);
        Map<UUID, Instant> lastAssignedMap = computeLastAssignedTimestamps(candidateIds);

        // 4. Sắp xếp theo tiêu chí:
        //    a. Số bài chờ duyệt ít nhất (Least-Loaded)
        //    b. Thời điểm gán gần nhất xa nhất trong quá khứ (Round-Robin / Longest idle)
        //    c. Điểm đánh giá (Rating) cao hơn
        //    d. User ID (ổn định thứ tự)
        Comparator<ExpertProfile> comparator = Comparator
                .<ExpertProfile, Long>comparing(ep -> workloadMap.getOrDefault(ep.getUserId(), 0L))
                .thenComparing(ep -> lastAssignedMap.get(ep.getUserId()), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ExpertProfile::getRatingAvg, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExpertProfile::getUserId);

        ExpertProfile chosen = candidatePool.stream()
                .min(comparator)
                .orElse(candidatePool.getFirst());

        log.info("Dispatched content (type={}, stage={}) to CONTRACTED expert {} (current pending load={})",
                type, stage, chosen.getUserId(), workloadMap.getOrDefault(chosen.getUserId(), 0L));

        return chosen.getUserId();
    }

    private List<ExpertProfile> filterBySpecialty(List<ExpertProfile> experts, ContentStage stage) {
        if (stage == null) {
            return experts;
        }

        return experts.stream()
                .filter(ep -> matchesStageSpecialty(ep.getSpecialty(), stage))
                .toList();
    }

    private boolean matchesStageSpecialty(String specialty, ContentStage stage) {
        if (specialty == null || specialty.isBlank()) {
            return false;
        }
        String s = specialty.toLowerCase();
        return switch (stage) {
            case PRE_PREGNANCY, PREGNANCY, POSTPARTUM ->
                s.contains("sản") || s.contains("phụ") || s.contains("obstetric") || s.contains("gynec") || s.contains("matern");
            case BABY_CARE ->
                s.contains("nhi") || s.contains("pediatric") || s.contains("child") || s.contains("baby") || s.contains("sơ sinh");
        };
    }

    private Map<UUID, Long> computeActiveWorkload(Set<UUID> expertIds) {
        Map<UUID, Long> workload = new HashMap<>();
        expertIds.forEach(id -> workload.put(id, 0L));

        // Đếm bài viết/FAQ pending
        for (Object[] row : contentRepository.countPendingByAssignedExpertIds(ContentStatus.PENDING_REVIEW, expertIds)) {
            UUID expertId = (UUID) row[0];
            Long count = (Long) row[1];
            if (expertId != null) {
                workload.merge(expertId, count, Long::sum);
            }
        }

        // Đếm checklist templates pending
        for (Object[] row : checklistTemplateRepository.countPendingByAssignedExpertIds(ChecklistTemplateStatus.PENDING_REVIEW, expertIds)) {
            UUID expertId = (UUID) row[0];
            Long count = (Long) row[1];
            if (expertId != null) {
                workload.merge(expertId, count, Long::sum);
            }
        }

        return workload;
    }

    private Map<UUID, Instant> computeLastAssignedTimestamps(Set<UUID> expertIds) {
        Map<UUID, Instant> timestamps = new HashMap<>();
        for (UUID expertId : expertIds) {
            Instant t1 = contentRepository.findLatestAssignedAtByExpertId(expertId);
            Instant t2 = checklistTemplateRepository.findLatestAssignedAtByExpertId(expertId);
            Instant latest = null;
            if (t1 != null && t2 != null) {
                latest = t1.isAfter(t2) ? t1 : t2;
            } else if (t1 != null) {
                latest = t1;
            } else {
                latest = t2;
            }
            timestamps.put(expertId, latest);
        }
        return timestamps;
    }
}
