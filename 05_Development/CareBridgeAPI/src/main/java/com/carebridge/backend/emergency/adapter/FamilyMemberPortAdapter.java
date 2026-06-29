package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.FamilyMemberPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class FamilyMemberPortAdapter implements FamilyMemberPort {

    @Override
    public List<String> getFamilyFcmTokens(UUID userId) {
        log.warn("FamilyMemberPort not implemented — no FCM tokens returned for user {}", userId);
        return List.of();
    }
}
