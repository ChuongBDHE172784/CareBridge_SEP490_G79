package com.carebridge.backend.integration.trackasia;

import lombok.Data;

@Data
public class TrackAsiaPlaceDto {
    private String placeId;
    private String name;
    private String label;
    private Double longitude;
    private Double latitude;
}
