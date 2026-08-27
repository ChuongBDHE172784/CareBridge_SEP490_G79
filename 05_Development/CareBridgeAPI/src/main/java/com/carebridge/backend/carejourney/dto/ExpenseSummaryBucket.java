package com.carebridge.backend.carejourney.dto;

import java.math.BigDecimal;

public record ExpenseSummaryBucket(
        String groupKey,
        BigDecimal totalAmount,
        String currency,
        long count
) {}
