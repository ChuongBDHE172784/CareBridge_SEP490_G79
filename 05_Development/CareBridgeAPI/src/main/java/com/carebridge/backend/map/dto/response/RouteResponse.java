package com.carebridge.backend.map.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {
    private BigDecimal distanceMeters;
    private Integer etaMinutes;
    private Integer durationSeconds;
    private String encodedPolyline;
    private List<RoutePoint> points;
    private List<RouteStep> steps;
    private String transportMode;
}
