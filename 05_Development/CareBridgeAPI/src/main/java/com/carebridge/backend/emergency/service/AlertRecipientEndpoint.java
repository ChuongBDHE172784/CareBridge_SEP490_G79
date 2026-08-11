package com.carebridge.backend.emergency.service;

import java.util.UUID;

public record AlertRecipientEndpoint(UUID userId, UUID deviceTokenId, UUID careGroupId, String token) {}
