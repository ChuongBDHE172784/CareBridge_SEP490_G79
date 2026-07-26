package com.carebridge.backend.masterdata.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataDTO {
    private String id;
    private String name;
    private String description;
    private String category;
    private String region;
}
