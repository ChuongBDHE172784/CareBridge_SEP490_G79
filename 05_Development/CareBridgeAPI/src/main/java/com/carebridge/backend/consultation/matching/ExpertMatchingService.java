package com.carebridge.backend.consultation.matching;

import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.experttype.ExpertType;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hàm vét điều phối chuyên gia — xem docs/expert-matching-sweep.md.
 *
 * <p>Một cổng vào duy nhất, hai tuyến bên trong:
 * <ol>
 *   <li>Tuyến 1 — Chuyên gia Hệ thống ({@link ExpertType#CONTRACTED});</li>
 *   <li>Tuyến 2 — Chuyên gia Y tế Cộng đồng, CHỈ chạy khi tuyến 1 rỗng.</li>
 * </ol>
 *
 * <p>Mỗi tuyến có hai kiểu ứng viên:
 * <ul>
 *   <li>{@link Kind#SLOT} — chuyên gia đã set lịch, đặt theo ca 1 giờ cụ thể;</li>
 *   <li>{@link Kind#OPEN} — chuyên gia chưa set ca nào, nhận yêu cầu mở
 *       ({@code preferredWindow = null}).</li>
 * </ul>
 * SLOT luôn xếp trên OPEN: một khung giờ cụ thể có giá trị hơn lời hứa "sẽ phản hồi trong 48 giờ".
 *
 * <p>Hàm này chỉ <b>gợi ý</b>. Người dùng vẫn phải tạo yêu cầu tư vấn và chuyên gia vẫn phải bấm
 * chấp nhận — luồng đăng ký/chấp nhận hiện có giữ nguyên, không bị bỏ qua.
 *
 * <p>Không tạo bảng mới: chỉ đọc {@code expert_availability}, {@code users} và
 * {@code expert_consultation_requests} qua các repository sẵn có.
 */
@Service
public class ExpertMatchingService {

    /** Chỉ quét trong ngưỡng này để "nhiều giờ rảnh vào tuần sau" không thắng "rảnh chiều nay". */
    private static final int DEFAULT_SEARCH_WINDOW_HOURS = 24;

    /** Khối giờ ngắn hơn ngưỡng này bị loại — đây chính là luật "còn 5 phút thì bỏ qua". */
    private static final int DEFAULT_MIN_USABLE_MINUTES = 30;

    private static final int DEFAULT_LIMIT = 5;

    private final ExpertAvailabilityRepository availabilityRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // Ứng dụng không đăng ký bean Clock nào; theo đúng pattern của ExpertAvailabilityServiceImpl,
    // constructor Spring dùng đồng hồ hệ thống còn constructor kia để test tiêm đồng hồ cố định.
    @Autowired
    public ExpertMatchingService(
            ExpertAvailabilityRepository availabilityRepository,
            ExpertProfileRepository expertProfileRepository,
            ConsultationRequestRepository consultationRequestRepository,
            UserRepository userRepository) {
        this(availabilityRepository, expertProfileRepository, consultationRequestRepository,
                userRepository, Clock.systemDefaultZone());
    }

    public ExpertMatchingService(
            ExpertAvailabilityRepository availabilityRepository,
            ExpertProfileRepository expertProfileRepository,
            ConsultationRequestRepository consultationRequestRepository,
            UserRepository userRepository,
            Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.expertProfileRepository = expertProfileRepository;
        this.consultationRequestRepository = consultationRequestRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ── Kết quả ────────────────────────────────────────────────────────

    public enum Kind {
        /** Đặt theo ca cụ thể; client gửi lại slotStart/slotEnd vào preferredWindow. */
        SLOT,
        /** Chuyên gia chưa set lịch; client gửi yêu cầu với preferredWindow bỏ trống. */
        OPEN
    }

    /** Một chuyên gia kèm cách đặt được với họ. Các trường ca là {@code null} khi kind = OPEN. */
    public record Candidate(
            Kind kind,
            UUID expertProfileId,
            String displayName,
            String specialty,
            String professionalTitle,
            String workplace,
            BigDecimal ratingAvg,
            ExpertType expertType,
            /** Ca cụ thể để client gửi lại khi tạo yêu cầu — luôn dài đúng 1 giờ. */
            UUID availabilityId,
            Instant slotStart,
            Instant slotEnd,
            /** Khối liền mạch chứa ca trên; dùng để xếp hạng SLOT. */
            Instant blockStart,
            Instant blockEnd,
            long blockMinutes,
            /** Khối bắt đầu trong vòng 60 phút tới. */
            boolean startingSoon,
            /** Số yêu cầu đang chờ — khoá cân tải cho ứng viên OPEN. */
            long openRequestCount) {}

    /**
     * @param tier      tuyến đã dùng; {@code null} khi không tìm được ai
     * @param fallback  true khi tuyến 1 rỗng và kết quả đến từ nhóm cộng đồng
     */
    public record SweepResult(
            ExpertType tier,
            boolean fallback,
            List<Candidate> candidates) {

        public static SweepResult none() {
            return new SweepResult(null, false, List.of());
        }
    }

    // ── Hàm vét ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SweepResult sweep(String specialty) {
        return sweep(specialty, DEFAULT_LIMIT, DEFAULT_SEARCH_WINDOW_HOURS, DEFAULT_MIN_USABLE_MINUTES);
    }

    @Transactional(readOnly = true)
    public SweepResult sweep(String specialty, int limit, int windowHours, int minUsableMinutes) {
        Instant now = clock.instant();
        String specialtyFilter = (specialty == null || specialty.isBlank()) ? null : specialty.trim();

        // B1-B2: ca đặt được → gộp thành khối liền mạch → giữ khối dài nhất mỗi chuyên gia.
        Map<UUID, Block> blocks = bestBlockPerExpert(
                availabilityRepository.findBookableSlots(
                        now, now.plus(Math.max(1, windowHours), ChronoUnit.HOURS), specialtyFilter),
                minUsableMinutes);

        // B3: chuyên gia chưa set ca nào → ứng viên yêu cầu mở.
        List<ExpertProfile> openProfiles =
                expertProfileRepository.findOpenRequestExperts(specialtyFilter, now);

        if (blocks.isEmpty() && openProfiles.isEmpty()) {
            return SweepResult.none();
        }

        Set<UUID> expertIds = new LinkedHashSet<>(blocks.keySet());
        openProfiles.forEach(profile -> expertIds.add(profile.getUserId()));

        // Cùng pattern batch-load của getPublicDirectory: 1 truy vấn cho cả trang, không N+1.
        Map<UUID, ExpertProfile> profiles = expertProfileRepository.findAllById(expertIds).stream()
                .collect(Collectors.toMap(ExpertProfile::getUserId, profile -> profile));
        Map<UUID, User> users = userRepository.findAllById(expertIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<UUID, Long> pendingCounts = pendingCounts(expertIds);

        List<Candidate> contracted = new ArrayList<>();
        List<Candidate> community = new ArrayList<>();
        for (UUID expertId : expertIds) {
            ExpertProfile profile = profiles.get(expertId);
            if (profile == null) {
                continue;
            }
            Block block = blocks.get(expertId);
            Candidate candidate = block != null
                    ? slotCandidate(profile, users.get(expertId), block, now,
                            pendingCounts.getOrDefault(expertId, 0L))
                    : openCandidate(profile, users.get(expertId),
                            pendingCounts.getOrDefault(expertId, 0L));
            (profile.isContracted() ? contracted : community).add(candidate);
        }

        // B4: tuyến 2 chỉ chạy khi tuyến 1 rỗng — nhóm cộng đồng không bao giờ bị trộn lẫn vào
        // danh sách khi vẫn còn Chuyên gia Hệ thống nhận được ca.
        if (!contracted.isEmpty()) {
            return new SweepResult(ExpertType.CONTRACTED, false, rank(contracted, limit));
        }
        if (!community.isEmpty()) {
            return new SweepResult(ExpertType.COMMUNITY, true, rank(community, limit));
        }
        return SweepResult.none();
    }

    // ── Nội bộ ─────────────────────────────────────────────────────────

    /** Khối giờ rảnh liền mạch: các ca 1 tiếng nối đuôi nhau được gộp làm một. */
    private record Block(List<ExpertAvailability> slots, Instant start, Instant end) {
        long minutes() {
            return Duration.between(start, end).toMinutes();
        }
    }

    private Map<UUID, Long> pendingCounts(Set<UUID> expertIds) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : consultationRequestRepository.countPendingByExpert(expertIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /**
     * Gộp ca liền nhau thành khối, rồi giữ lại khối DÀI NHẤT của mỗi chuyên gia.
     *
     * <p>Đây là chỗ thực thi yêu cầu "ưu tiên thằng nhiều thời gian hơn": một chuyên gia còn đúng
     * một ca lẻ sẽ thua chuyên gia còn ba ca liền mạch, dù ca lẻ kia bắt đầu sớm hơn.
     */
    private static Map<UUID, Block> bestBlockPerExpert(
            List<ExpertAvailability> slots, int minUsableMinutes) {
        Map<UUID, List<ExpertAvailability>> byExpert = new LinkedHashMap<>();
        for (ExpertAvailability slot : slots) {
            byExpert.computeIfAbsent(slot.getExpertProfileId(), key -> new ArrayList<>()).add(slot);
        }

        Map<UUID, Block> best = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<ExpertAvailability>> entry : byExpert.entrySet()) {
            List<ExpertAvailability> ordered = entry.getValue().stream()
                    .sorted(Comparator.comparing(ExpertAvailability::getStartAt))
                    .toList();

            List<ExpertAvailability> current = new ArrayList<>();
            Block longest = null;
            for (ExpertAvailability slot : ordered) {
                boolean contiguous = !current.isEmpty()
                        && current.getLast().getEndAt().equals(slot.getStartAt());
                if (!contiguous && !current.isEmpty()) {
                    longest = keepLonger(longest, toBlock(current));
                    current = new ArrayList<>();
                }
                current.add(slot);
            }
            if (!current.isEmpty()) {
                longest = keepLonger(longest, toBlock(current));
            }
            if (longest != null && longest.minutes() >= minUsableMinutes) {
                best.put(entry.getKey(), longest);
            }
        }
        return best;
    }

    private static Block toBlock(List<ExpertAvailability> slots) {
        return new Block(List.copyOf(slots), slots.getFirst().getStartAt(), slots.getLast().getEndAt());
    }

    private static Block keepLonger(Block a, Block b) {
        if (a == null) {
            return b;
        }
        if (b.minutes() != a.minutes()) {
            return b.minutes() > a.minutes() ? b : a;
        }
        return b.start().isBefore(a.start()) ? b : a;
    }

    private static Candidate slotCandidate(
            ExpertProfile profile, User user, Block block, Instant now, long pending) {
        ExpertAvailability first = block.slots().getFirst();
        return new Candidate(
                Kind.SLOT,
                profile.getUserId(),
                user != null ? user.getName() : null,
                profile.getSpecialty(),
                profile.getProfessionalTitle(),
                profile.getWorkplace(),
                profile.getRatingAvg(),
                profile.getExpertType(),
                first.getAvailabilityId(),
                first.getStartAt(),
                first.getEndAt(),
                block.start(),
                block.end(),
                block.minutes(),
                !block.start().isAfter(now.plus(60, ChronoUnit.MINUTES)),
                pending);
    }

    private static Candidate openCandidate(ExpertProfile profile, User user, long pending) {
        return new Candidate(
                Kind.OPEN,
                profile.getUserId(),
                user != null ? user.getName() : null,
                profile.getSpecialty(),
                profile.getProfessionalTitle(),
                profile.getWorkplace(),
                profile.getRatingAvg(),
                profile.getExpertType(),
                null, null, null, null, null,
                0L,
                false,
                pending);
    }

    /**
     * SLOT trước OPEN, rồi xếp trong từng nhóm.
     *
     * <p>SLOT: nhiều giờ rảnh trước, rồi mới tới bắt đầu sớm, cuối cùng là điểm đánh giá. Muốn đổi
     * sang ưu tiên "ca gần nhất" thì đảo hai khoá đầu — chỉ một dòng.
     *
     * <p>OPEN: ít yêu cầu đang chờ nhất trước. Thiếu khoá này, chuyên gia điểm cao nhất sẽ ôm toàn
     * bộ yêu cầu mở và những người còn lại không bao giờ được gợi ý.
     */
    private static List<Candidate> rank(List<Candidate> candidates, int limit) {
        Comparator<Candidate> bySlot = Comparator
                .comparingLong(Candidate::blockMinutes).reversed()
                .thenComparing(Candidate::slotStart);
        Comparator<Candidate> byLoad = Comparator.comparingLong(Candidate::openRequestCount);
        Comparator<Candidate> byRating =
                Comparator.comparing(Candidate::ratingAvg, Comparator.nullsLast(Comparator.reverseOrder()));

        return candidates.stream()
                .sorted(Comparator
                        .<Candidate, Integer>comparing(candidate -> candidate.kind() == Kind.SLOT ? 0 : 1)
                        .thenComparing((left, right) -> left.kind() == Kind.SLOT
                                ? bySlot.compare(left, right)
                                : byLoad.compare(left, right))
                        .thenComparing(byRating))
                .limit(Math.max(1, limit))
                .toList();
    }
}
