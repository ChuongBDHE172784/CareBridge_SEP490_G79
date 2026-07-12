package com.carebridge.backend.partner.service;

import com.carebridge.backend.partner.dto.request.RemovalRequest;
import com.carebridge.backend.partner.dto.response.RemovalResponse;
import com.carebridge.backend.partner.entity.PartnerContentTargetType;
import java.util.UUID;

public interface PartnerContentRemovalService {
    RemovalResponse remove(PartnerContentTargetType type, UUID targetId, RemovalRequest request, UUID adminId);
}
