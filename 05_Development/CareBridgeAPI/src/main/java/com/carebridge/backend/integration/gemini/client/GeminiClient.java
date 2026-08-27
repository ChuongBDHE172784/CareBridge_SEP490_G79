package com.carebridge.backend.integration.gemini.client;

import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;

/**
 * Sends a prompt to Gemini API and returns the text response.
 * Throws GeminiUnavailableException if API is unavailable or times out.
 */
public interface GeminiClient {

    String generate(String prompt) throws GeminiUnavailableException;
}
