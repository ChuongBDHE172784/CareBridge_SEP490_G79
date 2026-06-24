package com.carebridge.backend.integration.gemini.filter;

import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;

/**
 * Checks if a query contains red-flag patterns requiring emergency routing.
 */
public interface RagSafetyFilter {

    RagSafetyResult check(String query);
}
