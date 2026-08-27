package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LogTypeSummary {

    private int count;
    private BigDecimal totalQuantity;
    private String unit;
    private BigDecimal maxValue;
    private String latestNote;
    private List<String> notes;
}
