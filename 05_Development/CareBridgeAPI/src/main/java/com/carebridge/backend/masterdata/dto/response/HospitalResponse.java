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
public class HospitalResponse {
 private String hospitalId;
 private String name;
 private String provinceId;
 private String districtId;
 private String address;
 private String level;
 private String type;
 private String phone;
}
