package com.carebridge.backend.carejourney.repository;

import java.math.BigDecimal;

public interface LogTypeAggregateRow {
    String getLogType();
    long getCount();
    BigDecimal getTotalQuantity();
    BigDecimal getMaxQuantity();
    String getUnit();
}
