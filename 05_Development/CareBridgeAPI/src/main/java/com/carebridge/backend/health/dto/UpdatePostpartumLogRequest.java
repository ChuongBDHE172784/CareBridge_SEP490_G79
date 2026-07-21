package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.BleedingLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdatePostpartumLogRequest {

    @PastOrPresent
    private LocalDate logDate;

    @Min(0)
    @Max(10)
    private Short painLevel;

    private BleedingLevel bleedingLevel;

    @Min(0)
    @Max(10)
    private Short moodLevel;

    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private BigDecimal sleepHours;

    @Size(max = 1000)
    private String breastfeedingNote;

    @Size(max = 2000)
    private String symptomNote;

    @JsonIgnore
    private boolean breastfeedingNotePresent;

    @JsonIgnore
    private boolean symptomNotePresent;

    @JsonSetter("breastfeedingNote")
    public void setBreastfeedingNote(String breastfeedingNote) {
        this.breastfeedingNote = breastfeedingNote;
        this.breastfeedingNotePresent = true;
    }

    @JsonSetter("symptomNote")
    public void setSymptomNote(String symptomNote) {
        this.symptomNote = symptomNote;
        this.symptomNotePresent = true;
    }
}
