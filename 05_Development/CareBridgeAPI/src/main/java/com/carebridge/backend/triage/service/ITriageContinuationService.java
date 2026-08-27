package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.response.ContinuationDescriptor;
import java.util.UUID;

public interface ITriageContinuationService {
    ContinuationDescriptor resolve(UUID ownerUserId, String token);
    void acknowledge(UUID ownerUserId, String token);
}
