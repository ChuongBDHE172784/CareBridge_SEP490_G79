package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.SafetyEventType;

public record FallAnalysisResult(boolean suspected, SafetyEventType eventType, double magnitude) {}
