package com.carebridge.backend.content.dto.response;

import java.util.UUID;

public record ContentTagResponse(
    UUID id,
    String name,
    String slug
) {}
