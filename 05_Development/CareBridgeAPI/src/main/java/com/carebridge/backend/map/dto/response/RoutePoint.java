package com.carebridge.backend.map.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoint {
    private double lat;
    private double lng;
    private String instruction;
}
