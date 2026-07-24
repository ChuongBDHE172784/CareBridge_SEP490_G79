package com.carebridge.backend.baby.policy;

import com.carebridge.backend.triage.TriageStage;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Mirrors the production Baby Profile entry classifier using completed calendar months. */
@Component
public class BabyTriageStageClassifier {
    private final Clock clock;

    @Autowired
    public BabyTriageStageClassifier() {
        this(Clock.systemDefaultZone());
    }

    public BabyTriageStageClassifier(Clock clock) {
        this.clock = clock;
    }

    public TriageStage classify(LocalDate birthDate) {
        LocalDate today = LocalDate.now(clock);
        if (birthDate == null || birthDate.isAfter(today)) {
            return null;
        }
        int ageMonths = (today.getYear() - birthDate.getYear()) * 12
                + today.getMonthValue() - birthDate.getMonthValue();
        if (today.getDayOfMonth() < birthDate.getDayOfMonth()) {
            ageMonths--;
        }
        if (ageMonths < 12) {
            return TriageStage.INFANT;
        }
        if (ageMonths <= 24) {
            return TriageStage.TODDLER;
        }
        return null;
    }
}
