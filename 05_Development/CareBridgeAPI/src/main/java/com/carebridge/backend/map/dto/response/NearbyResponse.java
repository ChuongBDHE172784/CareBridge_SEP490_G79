package com.carebridge.backend.map.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyResponse {
    private List<FacilityResponse> facilities;
    private Integer totalCount;
}
