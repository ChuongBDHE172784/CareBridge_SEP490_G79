package com.carebridge.backend.masterdata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceResponse {
 private String provinceId;
 private String name;
 private String nameEn;
 private String region;
}
