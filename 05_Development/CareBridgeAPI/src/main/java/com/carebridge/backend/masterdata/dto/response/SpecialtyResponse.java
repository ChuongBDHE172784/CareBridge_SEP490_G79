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
public class SpecialtyResponse {
 private String specialtyId;
 private String name;
 private String description;
 private String category;
}
