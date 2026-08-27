package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.BabyCareOverviewResponse;
import java.util.UUID;

public interface IBabyCareOverviewService {
    BabyCareOverviewResponse getOverview(UUID babyId, UUID callerId);
}
