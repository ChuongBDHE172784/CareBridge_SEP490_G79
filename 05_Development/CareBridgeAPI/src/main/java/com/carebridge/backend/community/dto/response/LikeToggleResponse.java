package com.carebridge.backend.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeToggleResponse {

    private boolean liked;
    private int likeCount;
    private UUID answerId;
}
