package com.carebridge.backend.emergency.service;

import java.util.List;
import java.util.UUID;

public interface FamilyMemberPort {
    List<String> getFamilyFcmTokens(UUID userId);
}
