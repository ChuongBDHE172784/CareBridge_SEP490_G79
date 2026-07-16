package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.service.HealthMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthMemoryServiceImpl implements HealthMemoryService {
    private final HealthMemoryEntryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<HealthMemoryEntry> list(UUID userId, TriageStage stage, UUID profileId) {
        if (profileId == null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-014", "A matching profile is required for health memory");
        }
        Instant now = Instant.now();
        return stage.isMaternal()
                ? repository.findActiveMaternal(userId, profileId, stage, now)
                : repository.findActivePediatric(userId, profileId, stage, now);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID entryId) {
        HealthMemoryEntry entry = repository.findByIdAndUserIdAndDeletedAtIsNull(entryId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-015", "Health memory entry not found"));
        entry.setDeletedAt(Instant.now());
        repository.save(entry);
    }
}
