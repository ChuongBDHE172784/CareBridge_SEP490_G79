package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.policy.TriageFollowUpTitlePolicy;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.ITriageFollowUpService;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * CB-TYFU-IMP-001 — creates the YELLOW triage follow-up care item in the canonical
 * {@code scheduled_care_items} table (ADR-TYFU-001..006).
 *
 * <p>Log hygiene (TDS §4.3 / §14.2): only sessionId, outcome codes and exception
 * class names are logged — never symptom text or titles.
 *
 * @version 1.0
 */
@Service
public class TriageFollowUpService implements ITriageFollowUpService {

    private static final Logger log = LoggerFactory.getLogger(TriageFollowUpService.class);

    static final String SOURCE_REFERENCE_TYPE = "TRIAGE_SESSION";
    private static final long DEFAULT_DELAY_HOURS = 4L;   // ADR-TYFU-005 (Open O1 → default 4)
    private static final long MIN_DELAY_HOURS = 1L;
    private static final long MAX_DELAY_HOURS = 24L;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    /**
     * ADR-TYFU-006 (b): keyword table mirroring {@code SymptomNormalizer.KEYWORDS}
     * semantics for the free-text fallback path (Logic Issue L5 — {@code symptom_list}
     * may not be extracted yet at handler time). Only text-matchable codes are listed.
     */
    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("fever", List.of("sot", "nong", "temperature", "fever")),
            Map.entry("cough", List.of(" ho ", "cough")),
            Map.entry("runny_nose", List.of("so mui", "chay mui", "runny")),
            Map.entry("difficulty_breathing", List.of("kho tho", "tho gap", "wheeze")),
            Map.entry("vomiting", List.of("non", "oi", "vomit", "vomiting")),
            Map.entry("persistent_vomiting", List.of("non lien tuc", "non nhieu", "vomiting everything")),
            Map.entry("diarrhea", List.of("tieu chay", "diarrhea")),
            Map.entry("mild_dehydration", List.of("mat nuoc", "moi kho", "tieu it")),
            Map.entry("severe_dehydration", List.of("mat nuoc nang", "khoc khong co nuoc mat", "mat trung")));

    private final ReminderRepository reminderRepository;
    private final IIntakeSessionRepository intakeSessionRepository;
    private final TriageFollowUpTitlePolicy titlePolicy;
    private final INotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;
    private final long delayHours;

    @Autowired
    public TriageFollowUpService(
            ReminderRepository reminderRepository,
            IIntakeSessionRepository intakeSessionRepository,
            TriageFollowUpTitlePolicy titlePolicy,
            INotificationService notificationService,
            AuditService auditService,
            @Value("${carebridge.triage.follow-up.delay-hours:4}") long delayHours) {
        this(reminderRepository, intakeSessionRepository, titlePolicy, notificationService,
                auditService, delayHours, Clock.systemUTC());
    }

    /** Test constructor — deterministic time via fixed Clock (ADR-TYFU-005). */
    public TriageFollowUpService(
            ReminderRepository reminderRepository,
            IIntakeSessionRepository intakeSessionRepository,
            TriageFollowUpTitlePolicy titlePolicy,
            INotificationService notificationService,
            AuditService auditService,
            long delayHours,
            Clock clock) {
        this.reminderRepository = reminderRepository;
        this.intakeSessionRepository = intakeSessionRepository;
        this.titlePolicy = titlePolicy;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.clock = clock;
        this.delayHours = resolveDelayHours(delayHours);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> scheduleFollowUp(IntakeSessionCompleted event) {
        // 1) Idempotency probe FIRST — cheapest, keeps the skip path side-effect free
        //    (BR-TYFU-002 / ADR-TYFU-003; eventId is random per publish — L3).
        if (reminderRepository.existsByReminderTypeAndSourceReferenceId(
                ReminderType.TRIAGE_FOLLOW_UP, event.sessionId())) {
            log.info("Triage follow-up skipped code=TYFU-002 outcome=ALREADY_SCHEDULED sessionId={}",
                    event.sessionId());
            return Optional.empty();
        }

        // 2) Session lookup — committed before AFTER_COMMIT, absence is anomalous (TYFU-001).
        Optional<IntakeSession> sessionLookup = intakeSessionRepository.findById(event.sessionId());
        if (sessionLookup.isEmpty()) {
            log.warn("Triage session not found for follow-up code=TYFU-001 sessionId={}",
                    event.sessionId());
            return Optional.empty();
        }
        IntakeSession session = sessionLookup.get();

        // 3) Title from canonical codes only — never raw text (ADR-TYFU-006 / BR-TYFU-004).
        String title = titlePolicy.deriveTitle(extractCanonicalSymptoms(session));

        // 4) scheduled_at = completedAt + delay; clock fallback (ADR-TYFU-005).
        Instant base = event.completedAt() != null ? event.completedAt() : clock.instant();
        Instant scheduledAt = base.plus(Duration.ofHours(delayHours));

        // 5) Persist PENDING item (TDS §5.2 row table; §6.3 invariant 1).
        Reminder saved = reminderRepository.save(Reminder.builder()
                .ownerUserId(event.userId())
                .journeyId(session.getJourneyId())
                .babyId(session.getBabyProfileId())
                .reminderType(ReminderType.TRIAGE_FOLLOW_UP)
                .title(title)
                .scheduledAt(scheduledAt)
                .recurrenceType(RecurrenceType.NONE)
                .status(ReminderStatus.PENDING)
                .sourceReferenceType(SOURCE_REFERENCE_TYPE)
                .sourceReferenceId(event.sessionId())
                .build());

        // 6) Push scheduling — failure keeps the item, fcm_job_id null (ADR-TYFU-004 / TYFU-004).
        try {
            String fcmJobId = notificationService.scheduleFcmPush(
                    event.userId(), title, "Nhắc theo dõi: " + title, scheduledAt);
            saved.setFcmJobId(fcmJobId);
            saved = reminderRepository.save(saved);
        } catch (RuntimeException exception) {
            log.warn("Push scheduling failed; item saved without fcm job code=TYFU-004 "
                            + "sessionId={} reason={}",
                    event.sessionId(), exception.getClass().getSimpleName());
        }

        // 7) Audit — ReminderServiceImpl convention (Luật 91/2025).
        auditService.log(AuditAction.REMINDER_CREATED, event.userId(),
                "Reminder", saved.getId().toString(), "triage follow-up");

        log.info("Triage follow-up scheduled outcome=FOLLOW_UP_SCHEDULED sessionId={} careItemId={}",
                event.sessionId(), saved.getId());
        return Optional.of(saved.getId());
    }

    /** ADR-TYFU-005 — values outside [1..24] fall back to the default with WARN TYFU-005. */
    private static long resolveDelayHours(long configured) {
        if (configured < MIN_DELAY_HOURS || configured > MAX_DELAY_HOURS) {
            log.warn("Invalid follow-up delay config; using default code=TYFU-005 configured={} default={}",
                    configured, DEFAULT_DELAY_HOURS);
            return DEFAULT_DELAY_HOURS;
        }
        return configured;
    }

    /**
     * Resolves canonical symptom codes from the session's free text by keyword match
     * (mirrors {@code SymptomNormalizer} semantics — accent-stripped, space-padded).
     * Returns an empty list when nothing matches → generic title fallback.
     */
    private static List<String> extractCanonicalSymptoms(IntakeSession session) {
        String symptoms = session.getSymptoms();
        if (symptoms == null || symptoms.isBlank()) {
            return List.of();
        }
        String text = " " + stripAccents(symptoms).replaceAll("\\s+", " ") + " ";
        List<String> canonical = new ArrayList<>();
        for (var entry : KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(text::contains)) {
                canonical.add(entry.getKey());
            }
        }
        return canonical;
    }

    private static String stripAccents(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }
}
