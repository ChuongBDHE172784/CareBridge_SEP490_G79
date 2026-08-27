package com.carebridge.backend.carejourney.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummaryResponse(
        String groupBy,
        List<ExpenseSummaryBucket> buckets,
        BigDecimal grandTotal,
        String grandTotalCurrency
) {}
