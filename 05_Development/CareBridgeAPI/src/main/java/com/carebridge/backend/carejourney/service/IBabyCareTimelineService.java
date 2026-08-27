package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.BabyCareTimelineResponse;
import java.util.UUID;

public interface IBabyCareTimelineService {
    BabyCareTimelineResponse getTimeline(UUID babyId, String cursor, int size, UUID callerId);
}
