package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.dto.JourneyTimelinePageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface IJourneyTimelineService {
    JourneyTimelinePageResponse getTimeline(UUID ownerUserId, UUID journeyId, Pageable pageable);
}
