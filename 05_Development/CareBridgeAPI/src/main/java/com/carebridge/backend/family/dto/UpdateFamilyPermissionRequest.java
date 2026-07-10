package com.carebridge.backend.family.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFamilyPermissionRequest {

    private Boolean calendar;
    private Boolean logs;
    private Boolean alerts;
    private Boolean records;

    public boolean hasAtLeastOneField() {
        return calendar != null || logs != null || alerts != null || records != null;
    }
}
