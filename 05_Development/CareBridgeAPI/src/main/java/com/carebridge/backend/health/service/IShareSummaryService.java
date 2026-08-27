package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.ShareSummaryRequest;
import com.carebridge.backend.health.dto.ShareSummaryResponse;

import java.util.UUID;

public interface IShareSummaryService {

    ShareSummaryResponse shareSummary(ShareSummaryRequest request, UUID motherUserId);
}
