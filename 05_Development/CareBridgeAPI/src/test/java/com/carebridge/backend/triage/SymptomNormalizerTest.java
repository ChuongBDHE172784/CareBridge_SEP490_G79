package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.engine.SymptomNormalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TriageSymptomSynonymExpansion — CB-TRIAGE-IMP-005 / CB-TRIAGE-IMP-005-TEST.
 *
 * <p>Oracle: TDS §5.3 Synonym Additions Table, rows S2, S4–S13 only (S1 bulging_fontanelle
 * DEFERRED per ADR-TSSE-001; S3 documented no-op, see TSSE-TC-09). SYNTHETIC data only.</p>
 *
 * <p>CASE 2.0 Props Isolation: fresh {@link SymptomNormalizer} and fresh request per use;
 * no shared mutable state (anti AP-AI-002).</p>
 */
class SymptomNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    static SymptomNormalizer makeNormalizer() {
        return new SymptomNormalizer(); // stateless component, fresh per test anyway
    }

    // FX-SYN-04 — minimal request, ONLY free text set
    static RunIntakeRequest makeFreeTextRequest(String parentFreeText) {
        RunIntakeRequest request = new RunIntakeRequest();
        request.setParentFreeText(parentFreeText);
        return request;
    }

    // FX-SYN-01 — oracle rows copied verbatim from TDS CB-TRIAGE-IMP-005 §5.3 (S2, S4–S13)
    record SynonymRow(String diacritic, String stripped, String canonical) {}

    static List<SynonymRow> synonymRows() {
        return List.of(
                new SynonymRow("trớ sữa", "tro sua", "vomiting"),
                new SynonymRow("hâm hấp", "ham hap", "fever"),
                new SynonymRow("lừ đừ", "lu du", "lethargy"),
                new SynonymRow("sụt sịt", "sut sit", "runny_nose"),
                new SynonymRow("khò khè", "kho khe", "difficulty_breathing"),
                new SynonymRow("thở rít", "tho rit", "difficulty_breathing"),
                new SynonymRow("biếng ăn", "bieng an", "poor_feeding"),
                new SynonymRow("ọc sữa", "oc sua", "vomiting"),
                new SynonymRow("đi ngoài", "di ngoai", "diarrhea"),
                new SynonymRow("ỉa chảy", "ia chay", "diarrhea"),
                new SynonymRow("rôm sảy", "rom say", "rash"));
    }

    // FX-SYN-03 — golden list, one representative existing keyword per current canonical code,
    // copied verbatim from the baseline SymptomNormalizer.KEYWORDS (17 codes).
    static List<SynonymRow> goldenExistingRows() {
        return List.of(
                new SynonymRow(null, "sot", "fever"),
                new SynonymRow(null, "be ho nhieu", "cough"), // keyword is " ho " with spaces
                new SynonymRow(null, "so mui", "runny_nose"),
                new SynonymRow(null, "kho tho", "difficulty_breathing"),
                new SynonymRow(null, "rut lom", "chest_indrawing"),
                new SynonymRow(null, "tim tai", "cyanosis"),
                new SynonymRow(null, "co giat", "seizure"),
                new SynonymRow(null, "li bi", "lethargy"),
                new SynonymRow(null, "kho danh thuc", "difficult_to_wake"),
                new SynonymRow(null, "khong uong", "unable_to_drink"),
                new SynonymRow(null, "bo bu", "poor_feeding"),
                new SynonymRow(null, "non", "vomiting"),
                new SynonymRow(null, "non lien tuc", "persistent_vomiting"),
                new SynonymRow(null, "tieu chay", "diarrhea"),
                new SynonymRow(null, "phat ban", "rash"),
                new SynonymRow(null, "moi kho", "mild_dehydration"),
                new SynonymRow(null, "mat nuoc nang", "severe_dehydration"));
    }

    // TSSE-TC-01 — each new synonym (accent-stripped input) maps to its canonical code
    @Test
    void tc01_eachNewSynonymAccentStrippedInputMapsToCanonical() {
        for (SynonymRow row : synonymRows()) {
            List<String> codes = makeNormalizer().normalize(makeFreeTextRequest(row.stripped()));
            assertThat(codes)
                    .as("TSSE-TC-01 input '%s' should map to '%s' (TDS §5.3)", row.stripped(), row.canonical())
                    .contains(row.canonical());
        }
    }

    // TSSE-TC-02 — diacritic input forms are matched (accent-strip path)
    @Test
    void tc02_diacriticInputFormsAreMatched() {
        for (SynonymRow row : synonymRows()) {
            List<String> codes = makeNormalizer().normalize(makeFreeTextRequest(row.diacritic()));
            assertThat(codes)
                    .as("TSSE-TC-02 diacritic input '%s' should map to '%s'", row.diacritic(), row.canonical())
                    .contains(row.canonical());
        }
    }

    // TSSE-TC-03 — mixed-case input is matched (lower-casing path)
    @Test
    void tc03_mixedCaseInputIsMatched() {
        assertThat(makeNormalizer().normalize(makeFreeTextRequest("Trớ SỮA")))
                .as("TSSE-TC-03 'Trớ SỮA'").contains("vomiting");
        assertThat(makeNormalizer().normalize(makeFreeTextRequest("KHÒ khè")))
                .as("TSSE-TC-03 'KHÒ khè'").contains("difficulty_breathing");
        for (SynonymRow row : synonymRows()) {
            String upper = row.diacritic().toUpperCase(Locale.ROOT);
            assertThat(makeNormalizer().normalize(makeFreeTextRequest(upper)))
                    .as("TSSE-TC-03 upper-case input '%s' should map to '%s'", upper, row.canonical())
                    .contains(row.canonical());
        }
    }

    // TSSE-TC-04 — NO regression on the 17 existing canonical symptoms (regression guard,
    // expected PASS at red phase per Test-Spec §5.1)
    @Test
    void tc04_noRegressionOnExistingSeventeenCanonicalCodes() {
        for (SynonymRow row : goldenExistingRows()) {
            List<String> codes = makeNormalizer().normalize(makeFreeTextRequest(row.stripped()));
            assertThat(codes)
                    .as("TSSE-TC-04 golden keyword '%s' must still map to '%s'", row.stripped(), row.canonical())
                    .contains(row.canonical());
        }
    }

    // TSSE-TC-05 — numeric/structured rules unchanged (regression guard,
    // expected PASS at red phase per Test-Spec §5.1)
    @Test
    void tc05_numericAndStructuredRulesUnchanged() {
        RunIntakeRequest lowFever = new RunIntakeRequest();
        lowFever.setTemperatureC(37.5);
        assertThat(makeNormalizer().normalize(lowFever)).as("TSSE-TC-05 37.5C").contains("fever");

        RunIntakeRequest midFever = new RunIntakeRequest();
        midFever.setTemperatureC(38.9);
        List<String> midCodes = makeNormalizer().normalize(midFever);
        assertThat(midCodes).as("TSSE-TC-05 38.9C fever").contains("fever");
        assertThat(midCodes).as("TSSE-TC-05 38.9C no high_fever").doesNotContain("high_fever");

        RunIntakeRequest highFever = new RunIntakeRequest();
        highFever.setTemperatureC(39.0);
        assertThat(makeNormalizer().normalize(highFever))
                .as("TSSE-TC-05 39.0C").contains("fever", "high_fever");

        RunIntakeRequest seizure = new RunIntakeRequest();
        seizure.setSeizure(true);
        assertThat(makeNormalizer().normalize(seizure)).as("TSSE-TC-05 seizure flag").contains("seizure");

        RunIntakeRequest dehydration = new RunIntakeRequest();
        dehydration.setDehydrationSigns(List.of("mat nuoc nang"));
        assertThat(makeNormalizer().normalize(dehydration))
                .as("TSSE-TC-05 severe dehydration sign").contains("severe_dehydration");
    }

    // TSSE-TC-08 (Java side) — parity vectors shared with the Python service
    // (pattern copied from PediatricRedParityTest)
    @Test
    void tc08_allSynonymParityVectorsMatchSharedContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/triage/symptom_synonym_parity_vectors.json")) {
            assertThat(input).as("parity vector resource must exist").isNotNull();
            List<Map<String, Object>> vectors = objectMapper.readValue(input, new TypeReference<>() {});
            assertThat(vectors).as("one vector per implemented TDS §5.3 row").hasSize(11);
            for (Map<String, Object> vector : vectors) {
                String text = (String) vector.get("parentFreeText");
                List<String> expected = objectMapper.convertValue(vector.get("expectedCodes"), new TypeReference<>() {});
                List<String> codes = makeNormalizer().normalize(makeFreeTextRequest(text));
                assertThat(codes)
                        .as("TSSE-TC-08 vector '%s'", text)
                        .containsAll(expected);
            }
        }
    }

    // TSSE-TC-09 — S3 "sốt sình sịch" already normalizes to fever (documented no-op /
    // regression guard; expected PASS from birth, excluded from Red Gate — Logic Issue L3)
    @Test
    void tc09_sotSinhSichAlreadyNormalizesToFever() {
        assertThat(makeNormalizer().normalize(makeFreeTextRequest("sốt sình sịch")))
                .as("TSSE-TC-09 diacritic").contains("fever");
        assertThat(makeNormalizer().normalize(makeFreeTextRequest("sot sinh sich")))
                .as("TSSE-TC-09 stripped").contains("fever");
    }
}
