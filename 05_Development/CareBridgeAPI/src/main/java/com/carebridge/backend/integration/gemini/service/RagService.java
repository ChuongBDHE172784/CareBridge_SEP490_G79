package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;

/**
 * RAG (Retrieval-Augmented Generation) service interface.
 * Generates AI-assisted answers from approved ContentItem sources.
 *
 * SAFETY CONTRACT:
 * - Response always contains disclaimer (never null)
 * - Response never contains medication names or dosage (enforced by prompt)
 * - Red-flag queries trigger emergency guidance, not AI answer
 * - NEVER throws for Gemini failures — always returns fallback response
 */
public interface RagService {

    RagAnswerResponse generateAnswer(RagAnswerRequest request, RagExecutionContext context);
}
