package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import java.security.Principal;

public interface ModerationService {

    ModerationQueueResponse getModerationQueue(ModerationQueueFilter filter, Principal principal);
}
