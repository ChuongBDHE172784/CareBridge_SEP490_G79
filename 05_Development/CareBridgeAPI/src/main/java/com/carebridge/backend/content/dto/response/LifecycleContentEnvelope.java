package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;

public record LifecycleContentEnvelope<T>(ContentStage stage, T payload) {
}
