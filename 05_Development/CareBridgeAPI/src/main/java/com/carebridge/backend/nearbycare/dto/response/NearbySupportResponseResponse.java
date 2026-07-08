package com.carebridge.backend.nearbycare.dto.response;

import com.carebridge.backend.nearbycare.entity.NearbySupportResponse;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySupportResponseResponse {

    private UUID responseId;
    private UUID requestId;
    private UUID expertProfileId;
    private String action;
    private String note;
    private LocalDateTime respondedAt;
}
