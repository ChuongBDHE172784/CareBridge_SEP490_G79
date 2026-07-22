package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.expert.dto.response.ContributionPointResponse;
import com.carebridge.backend.expert.entity.ContributionPoint;
import com.carebridge.backend.expert.repository.ContributionPointRepository;
import com.carebridge.backend.expert.service.IContributionPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributionPointServiceImpl implements IContributionPointService {

    private final ContributionPointRepository repository;

    // Fixed sum method — accepts nullable userId to satisfy findByUserId null check in tests
    private int sumByUser(UUID userId) {
        if (userId == null) return 0;
        return repository.sumPointsByUserId(userId);
    }

    @Override
    @Transactional
    public void awardPoints(UUID userId, int points, String reason, String sourceType, UUID sourceId) {
        ContributionPoint cp = ContributionPoint.builder()
                .userId(userId)
                .points(points)
                .reason(reason)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .build();
        repository.save(cp);
    }

    @Override
    @Transactional
    public void awardPointsIfNotExists(UUID userId, int points, String reason, String sourceType, UUID sourceId) {
        if (sourceId == null) {
            // Fall back to non-idempotent if no sourceId provided
            awardPoints(userId, points, reason, sourceType, sourceId);
            return;
        }

        // Check if a record with this sourceId already exists for this user
        // Using optimistic locking via unique constraint on (userId, sourceType, sourceId)
        Optional<ContributionPoint> existing = repository.findByUserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId);
        if (existing.isPresent()) {
            // Already awarded - skip silently (idempotent)
            return;
        }

        ContributionPoint cp = ContributionPoint.builder()
                .userId(userId)
                .points(points)
                .reason(reason)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .build();
        repository.save(cp);
    }

    @Override
    public int getTotalPoints(UUID userId) {
        return sumByUser(userId);
    }

    @Override
    public List<ContributionPointResponse> getRecentPoints(UUID userId, int limit) {
        return repository.findByUserIdOrderByRecordedAtDesc(
                userId,
                org.springframework.data.domain.PageRequest.of(0, limit)
        ).stream()
                .map(cp -> new ContributionPointResponse(
                        cp.getPointRecordId(),
                        cp.getPoints(),
                        cp.getReason(),
                        cp.getSourceType(),
                        cp.getRecordedAt()
                ))
                .toList();
    }
}
