package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.PartnerContentTargetType;
import java.time.Instant;
import java.util.UUID;

public record RemovalResponse(PartnerContentTargetType targetType, UUID targetId, boolean isRemoved,
                              UUID removedByAdminId, String reason, Instant removedAt) {}
