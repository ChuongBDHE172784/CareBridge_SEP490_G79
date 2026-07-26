package com.carebridge.backend.expert.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProfessionalSpecialtyId implements Serializable {

    private UUID professionalProfileId;
    private UUID specialtyId;

    public ProfessionalSpecialtyId() {
    }

    public ProfessionalSpecialtyId(UUID professionalProfileId, UUID specialtyId) {
        this.professionalProfileId = professionalProfileId;
        this.specialtyId = specialtyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProfessionalSpecialtyId that)) {
            return false;
        }
        return Objects.equals(professionalProfileId, that.professionalProfileId)
            && Objects.equals(specialtyId, that.specialtyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(professionalProfileId, specialtyId);
    }
}
