package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.UnpublishRequest;
import com.carebridge.backend.content.dto.response.UnpublishResponse;
import java.util.UUID;

public interface ContentUnpublishService {
    UnpublishResponse unpublish(UUID id, UnpublishRequest request, UUID adminId);
}
