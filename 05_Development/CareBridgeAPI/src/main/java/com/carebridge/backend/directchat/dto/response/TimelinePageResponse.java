package com.carebridge.backend.directchat.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimelinePageResponse {
    private final List<TimelineItemResponse> items;
    private final String nextCursor;
    private final boolean hasMoreNewer;
    private final String previousCursor;
    private final boolean hasMoreOlder;
}
