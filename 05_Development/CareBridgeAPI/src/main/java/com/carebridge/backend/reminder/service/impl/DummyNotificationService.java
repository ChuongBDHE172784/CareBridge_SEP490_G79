package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.reminder.service.INotificationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DummyNotificationService implements INotificationService {

    @Override
    public String scheduleFcmPush(UUID userId, String title, String body, Instant scheduledAt) {
        return "dummy-job-id";
    }

    @Override
    public void cancelFcmJob(String fcmJobId) {
        // no-op in dummy implementation
    }
}
