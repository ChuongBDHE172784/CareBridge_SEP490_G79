package com.carebridge.backend.recommendation;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/** Frozen V1 vocabulary shared by validation, signal mapping, and editorial checks. */
public final class RecommendationConstants {

    private RecommendationConstants() {}

    public static final int SCHEMA_VERSION = 1;
    public static final String POLICY_VERSION = "MOTHER_PERSONALIZED_CONTENT_V1";
    public static final String CATALOG_VERSION = "RECOMMENDATION_TAG_CATALOG_V1";
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int DEFAULT_LIMIT = 3;
    public static final int MAX_LIMIT = 3;
    /** Per-pool safety bound for targeted and fallback retrieval. */
    public static final int MAX_POOL_SCAN = 500;

    public static final Set<String> DOMAINS = Set.of(
            "age", "bmi", "reproductiveHistory", "underlyingConditions", "lifestyle",
            "nutrition", "vaccination", "currentMedications", "sexualHealth", "sti");

    public static final Set<String> ANSWER_STATES = Set.of(
            "KNOWN", "UNKNOWN", "PREFER_NOT_TO_SAY", "NOT_APPLICABLE");

    public static final Map<String, String> AGE_SIGNALS = Map.of(
            "UNDER_18", "rec-age-under-18",
            "AGE_18_24", "rec-age-18-24",
            "AGE_25_34", "rec-age-25-34",
            "AGE_35_39", "rec-age-35-39",
            "AGE_40_PLUS", "rec-age-40-plus");

    public static final Map<String, String> BMI_SIGNALS = Map.of(
            "UNDERWEIGHT", "rec-bmi-underweight",
            "HEALTHY_RANGE", "rec-bmi-healthy-range",
            "OVERWEIGHT", "rec-bmi-overweight",
            "OBESITY", "rec-bmi-obesity");

    public static final Map<String, String> REPRODUCTIVE_SIGNALS = Map.ofEntries(
            Map.entry("NO_PRIOR_PREGNANCY", "rec-reproductive-no-prior-pregnancy"),
            Map.entry("PRIOR_LIVE_BIRTH", "rec-reproductive-prior-live-birth"),
            Map.entry("PRIOR_PREGNANCY_LOSS", "rec-reproductive-prior-pregnancy-loss"),
            Map.entry("PRIOR_RECURRENT_PREGNANCY_LOSS", "rec-reproductive-recurrent-pregnancy-loss"),
            Map.entry("PRIOR_STILLBIRTH", "rec-reproductive-prior-stillbirth"),
            Map.entry("PRIOR_PRETERM_BIRTH", "rec-reproductive-prior-preterm-birth"),
            Map.entry("PRIOR_MULTIPLE_PREGNANCY", "rec-reproductive-prior-multiple-pregnancy"),
            Map.entry("PRIOR_ECTOPIC_PREGNANCY", "rec-reproductive-ectopic-pregnancy"),
            Map.entry("PRIOR_PREECLAMPSIA", "rec-reproductive-preeclampsia"),
            Map.entry("PRIOR_GESTATIONAL_DIABETES", "rec-reproductive-gestational-diabetes"),
            Map.entry("OTHER_HISTORY", "rec-reproductive-other-history"));

    public static final Map<String, String> CONDITION_SIGNALS = Map.ofEntries(
            Map.entry("HYPERTENSION", "rec-condition-hypertension"),
            Map.entry("DIABETES", "rec-condition-diabetes"),
            Map.entry("THYROID_DISORDER", "rec-condition-thyroid-disorder"),
            Map.entry("CARDIOVASCULAR_DISEASE", "rec-condition-cardiovascular-disease"),
            Map.entry("ASTHMA", "rec-condition-asthma"),
            Map.entry("EPILEPSY", "rec-condition-epilepsy"),
            Map.entry("KIDNEY_DISEASE", "rec-condition-kidney-disease"),
            Map.entry("AUTOIMMUNE_DISEASE", "rec-condition-autoimmune-disease"),
            Map.entry("LUPUS", "rec-condition-lupus"),
            Map.entry("ANEMIA", "rec-condition-anemia"),
            Map.entry("PCOS", "rec-condition-pcos"),
            Map.entry("ENDOMETRIOSIS", "rec-condition-endometriosis"),
            Map.entry("INFERTILITY", "rec-condition-infertility"),
            Map.entry("MENTAL_HEALTH_CONDITION", "rec-condition-mental-health"),
            Map.entry("OTHER_CLINICIAN_CONFIRMED", "rec-condition-other-clinician-confirmed"));

    public static final Map<String, String> SMOKING_SIGNALS = Map.of(
            "NEVER", "rec-smoking-never", "FORMER", "rec-smoking-former", "CURRENT", "rec-smoking-current");
    public static final Map<String, String> ALCOHOL_SIGNALS = Map.of(
            "NONE", "rec-alcohol-none", "LESS_THAN_WEEKLY", "rec-alcohol-less-than-weekly",
            "WEEKLY_OR_MORE", "rec-alcohol-weekly-or-more", "ANY_USE", "rec-alcohol-use");
    public static final Map<String, String> ACTIVITY_SIGNALS = Map.of(
            "LOW", "rec-activity-low", "MODERATE", "rec-activity-moderate", "HIGH", "rec-activity-high");
    public static final Map<String, String> SLEEP_SIGNALS = Map.of(
            "NO_CONCERN", "rec-sleep-no-concern", "CONCERN", "rec-sleep-concern");

    /** Additional lifestyle flags introduced by the 9-group questionnaire. */
    public static final Map<String, String> LIFESTYLE_FLAG_SIGNALS = Map.of(
            "SUBSTANCE_USE", "rec-lifestyle-substance-use",
            "STRESS", "rec-lifestyle-stress",
            "UNHEALTHY_DIET", "rec-lifestyle-unhealthy-diet");

    public static final Map<String, String> NUTRITION_SIGNALS = Map.ofEntries(
            Map.entry("VEGETARIAN", "rec-nutrition-vegetarian"),
            Map.entry("VEGAN", "rec-nutrition-vegan"),
            Map.entry("FOOD_INSECURITY", "rec-nutrition-food-insecurity"),
            Map.entry("LOW_APPETITE", "rec-nutrition-low-appetite"),
            Map.entry("NAUSEA_OR_VOMITING", "rec-nutrition-nausea-or-vomiting"),
            Map.entry("IRON_OR_FOLATE_CONCERN", "rec-nutrition-iron-or-folate-concern"),
            Map.entry("FOLIC_ACID_NOT_STARTED", "rec-nutrition-folic-acid-needed"),
            Map.entry("IODINE_UNASSESSED_OR_INSUFFICIENT", "rec-nutrition-iodine-review"),
            Map.entry("VITAMIN_D_INSUFFICIENT_OR_SUPPLEMENT", "rec-nutrition-vitamin-d-review"),
            Map.entry("IRON_INSUFFICIENT_OR_SUPPLEMENT", "rec-nutrition-iron-review"),
            Map.entry("CALCIUM_INSUFFICIENT_OR_SUPPLEMENT", "rec-nutrition-calcium-review"),
            Map.entry("OTHER_NUTRITION_CONCERN", "rec-nutrition-other-concern"));

    public static final Map<String, String> VACCINATION_SIGNALS = Map.of(
            "INFLUENZA", "rec-vaccination-influenza-due", "COVID_19", "rec-vaccination-covid-19-due",
            "TDAP", "rec-vaccination-tdap-due", "HEPATITIS_B", "rec-vaccination-hepatitis-b-due",
            "RUBELLA_IMMUNITY", "rec-vaccination-rubella-immunity-review");
    public static final Map<String, String> VACCINATION_FLAG_SIGNALS = Map.of(
            "NOT_ASSESSED", "rec-vaccination-assessment-needed");

    public static final Map<String, String> MEDICATION_SIGNALS = Map.ofEntries(
            Map.entry("PRENATAL_VITAMIN", "rec-medication-prenatal-vitamin"),
            Map.entry("FOLIC_ACID", "rec-medication-folic-acid"),
            Map.entry("IRON", "rec-medication-iron"),
            Map.entry("THYROID_MEDICATION", "rec-medication-thyroid"),
            Map.entry("DIABETES_MEDICATION", "rec-medication-diabetes"),
            Map.entry("ANTIHYPERTENSIVE", "rec-medication-antihypertensive"),
            Map.entry("ANTICOAGULANT", "rec-medication-anticoagulant"),
            Map.entry("ANTIEPILEPTIC", "rec-medication-antiepileptic"),
            Map.entry("MENTAL_HEALTH_MEDICATION", "rec-medication-mental-health"),
            Map.entry("OTHER_PRESCRIBED", "rec-medication-other-prescribed"),
            Map.entry("HIGH_RISK_OR_CONTRAINDICATED", "rec-medication-high-risk-or-contraindicated"),
            Map.entry("NEEDS_ADJUSTMENT", "rec-medication-adjustment-needed"));

    public static final Map<String, String> SEXUAL_HEALTH_SIGNALS = Map.of(
            "GENERAL_INFORMATION", "rec-sexual-health-general-information",
            "CONTRACEPTION_OR_FERTILITY", "rec-sexual-health-contraception-or-fertility",
            "INTIMACY_DURING_LIFECYCLE", "rec-sexual-health-intimacy-during-lifecycle",
            "SAFE_SEX_COUNSELING_NEEDED", "rec-sexual-health-safe-sex-counseling",
            "STI_RISK", "rec-sexual-health-sti-risk",
            "REPRODUCTIVE_TRACT_INFECTION", "rec-sexual-health-reproductive-tract-infection",
            "STI_SUSPECTED_OR_KNOWN", "rec-sexual-health-sti-suspected-or-known",
            "NO_PREGNANCY_PLAN", "rec-sexual-health-no-pregnancy-plan",
            "OTHER_NON_URGENT_INFORMATION", "rec-sexual-health-other-non-urgent");

    public static final Map<String, String> STI_STATUS_SIGNALS = Map.of(
            "SCREENING_INFORMATION", "rec-sti-screening-information",
            "PAST_HISTORY", "rec-sti-past-history",
            "CURRENT_OR_UNDER_TREATMENT", "rec-sti-current-or-treatment",
            "AT_RISK", "rec-sti-risk",
            "SUSPECTED_OR_KNOWN", "rec-sti-suspected-or-known");
    public static final Map<String, String> STI_INFECTION_SIGNALS = Map.of(
            "HIV", "rec-sti-hiv", "SYPHILIS", "rec-sti-syphilis", "HEPATITIS_B", "rec-sti-hepatitis-b",
            "HEPATITIS_C", "rec-sti-hepatitis-c", "CHLAMYDIA", "rec-sti-chlamydia",
            "GONORRHEA", "rec-sti-gonorrhea", "HERPES", "rec-sti-herpes", "HPV", "rec-sti-hpv", "OTHER", "rec-sti-other");

    public static final Map<String, String> SUPPORT_PREFERENCE_SIGNALS = Map.of(
            "NUTRITION", "rec-preference-nutrition", "MENTAL_WELLBEING", "rec-preference-mental-wellbeing",
            "PHYSICAL_ACTIVITY", "rec-preference-physical-activity", "APPOINTMENT_REMINDERS", "rec-preference-appointment-reminders");

    public static final Set<String> VACCINE_CODES = Set.of("INFLUENZA", "COVID_19", "TDAP", "HEPATITIS_B", "RUBELLA_IMMUNITY");
    public static final Set<String> STI_INFECTION_CODES = STI_INFECTION_SIGNALS.keySet();
    public static final Set<String> ALL_TAG_SLUGS;
    public static final Map<String, String> EXCLUSIVE_TAG_GROUPS;

    static {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(AGE_SIGNALS.values());
        values.addAll(BMI_SIGNALS.values());
        values.addAll(REPRODUCTIVE_SIGNALS.values());
        values.addAll(CONDITION_SIGNALS.values());
        values.addAll(SMOKING_SIGNALS.values());
        values.addAll(ALCOHOL_SIGNALS.values());
        values.addAll(ACTIVITY_SIGNALS.values());
        values.addAll(SLEEP_SIGNALS.values());
        values.addAll(LIFESTYLE_FLAG_SIGNALS.values());
        values.addAll(NUTRITION_SIGNALS.values());
        values.addAll(VACCINATION_SIGNALS.values());
        values.addAll(VACCINATION_FLAG_SIGNALS.values());
        values.addAll(MEDICATION_SIGNALS.values());
        values.addAll(SEXUAL_HEALTH_SIGNALS.values());
        values.addAll(STI_STATUS_SIGNALS.values());
        values.addAll(STI_INFECTION_SIGNALS.values());
        values.addAll(SUPPORT_PREFERENCE_SIGNALS.values());
        ALL_TAG_SLUGS = Set.copyOf(values);
        Map<String, String> exclusive = new LinkedHashMap<>();
        AGE_SIGNALS.values().forEach(slug -> exclusive.put(slug, "AGE"));
        BMI_SIGNALS.values().forEach(slug -> exclusive.put(slug, "BMI"));
        SMOKING_SIGNALS.values().forEach(slug -> exclusive.put(slug, "SMOKING"));
        ALCOHOL_SIGNALS.values().forEach(slug -> exclusive.put(slug, "ALCOHOL"));
        ACTIVITY_SIGNALS.values().forEach(slug -> exclusive.put(slug, "ACTIVITY"));
        SLEEP_SIGNALS.values().forEach(slug -> exclusive.put(slug, "SLEEP"));
        STI_STATUS_SIGNALS.values().forEach(slug -> exclusive.put(slug, "STI_STATUS"));
        EXCLUSIVE_TAG_GROUPS = Map.copyOf(exclusive);
    }

    public static String tagFor(String domain, String value) {
        return switch (domain) {
            case "reproductiveHistory" -> REPRODUCTIVE_SIGNALS.get(value);
            case "underlyingConditions" -> CONDITION_SIGNALS.get(value);
            case "smoking" -> SMOKING_SIGNALS.get(value);
            case "alcohol" -> ALCOHOL_SIGNALS.get(value);
            case "physicalActivity" -> ACTIVITY_SIGNALS.get(value);
            case "sleep" -> SLEEP_SIGNALS.get(value);
            case "nutrition" -> NUTRITION_SIGNALS.get(value);
            case "currentMedications" -> MEDICATION_SIGNALS.get(value);
            case "sexualHealth" -> SEXUAL_HEALTH_SIGNALS.get(value);
            case "stiStatus" -> STI_STATUS_SIGNALS.get(value);
            case "stiInfection" -> STI_INFECTION_SIGNALS.get(value);
            default -> null;
        };
    }

    /** Mirrors md5('RECOMMENDATION_TAG_CATALOG_V1:' || slug)::uuid in the seed migration. */
    public static UUID catalogIdFor(String slug) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest((CATALOG_VERSION + ":" + slug).getBytes(StandardCharsets.UTF_8));
            ByteBuffer bytes = ByteBuffer.wrap(digest);
            return new UUID(bytes.getLong(), bytes.getLong());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 is required for the frozen catalog identity", ex);
        }
    }
}
