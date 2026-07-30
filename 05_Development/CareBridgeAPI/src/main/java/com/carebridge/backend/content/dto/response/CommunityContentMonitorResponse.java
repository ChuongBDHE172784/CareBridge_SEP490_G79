package com.carebridge.backend.content.dto.response;

import java.util.List;

/** A server-paginated page of APPROVED questions or answers visible to community members. */
public record CommunityContentMonitorResponse(
        List<CommunityContentMonitorItemResponse> content,
        long totalElements,
        int page,
        int size) {
}
