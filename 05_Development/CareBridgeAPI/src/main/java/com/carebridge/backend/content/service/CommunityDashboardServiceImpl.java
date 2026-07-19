package com.carebridge.backend.content.service;

import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.dto.response.CommunityDashboardResponse;
import com.carebridge.backend.content.dto.response.ContentCountMetrics;
import com.carebridge.backend.content.dto.response.ReportMetrics;
import com.carebridge.backend.content.dto.response.TrendingTopic;
import com.carebridge.backend.content.dto.response.UserMetrics;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommunityDashboardServiceImpl implements CommunityDashboardService {

    // TDS §5.2 Open item — recommended default: top-5 trending topics
    private static final int TRENDING_TOP_N = 5;
    // TDS §5.2 Open item — recommended default window: last 30 days
    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final CommunityQuestionRepository communityQuestionRepository;
    private final CommunityAnswerRepository communityAnswerRepository;
    private final ContentReportRepository contentReportRepository;

    @Override
    public CommunityDashboardResponse getDashboard(DashboardFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw ModerationException.invalidDateRange();
        }

        Instant now = Instant.now();
        // "to" is inclusive of the whole day — window end is the start of the NEXT day (exclusive upper bound)
        Instant to = filter.to() != null
                ? filter.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : now;
        Instant from = filter.from() != null
                ? filter.from().atStartOfDay(ZoneOffset.UTC).toInstant()
                : to.minus(Duration.ofDays(DEFAULT_WINDOW_DAYS));

        UserMetrics userMetrics = buildUserMetrics(now);
        ContentCountMetrics questionMetrics = buildQuestionMetrics(from, to);
        ContentCountMetrics answerMetrics = buildAnswerMetrics(from, to);
        ReportMetrics reportMetrics = buildReportMetrics();
        List<TrendingTopic> trendingTopics = buildTrendingTopics(from, to);

        return new CommunityDashboardResponse(
                userMetrics, questionMetrics, answerMetrics, reportMetrics, trendingTopics, Instant.now());
    }

    private UserMetrics buildUserMetrics(Instant now) {
        Map<String, Long> byRole = groupCountsToMap(userRepository.countGroupByRole());
        long total = byRole.values().stream().mapToLong(Long::longValue).sum();
        long active = userRepository.countActive(now);
        return new UserMetrics(total, byRole, active);
    }

    private ContentCountMetrics buildQuestionMetrics(Instant from, Instant to) {
        Map<String, Long> byStatus = groupCountsToMap(communityQuestionRepository.countGroupByStatus());
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long newInPeriod = communityQuestionRepository.countByCreatedAtBetween(from, to);
        return new ContentCountMetrics(total, byStatus, newInPeriod);
    }

    private ContentCountMetrics buildAnswerMetrics(Instant from, Instant to) {
        Map<String, Long> byStatus = groupCountsToMap(communityAnswerRepository.countGroupByStatus());
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long newInPeriod = communityAnswerRepository.countByCreatedAtBetween(from, to);
        return new ContentCountMetrics(total, byStatus, newInPeriod);
    }

    private ReportMetrics buildReportMetrics() {
        Map<String, Long> byStatus = groupCountsToMap(contentReportRepository.countGroupByStatus());

        List<Object[]> resolvedTimestamps = contentReportRepository.findResolvedTimestamps();
        Double avgHandlingTimeSeconds = null;
        if (!resolvedTimestamps.isEmpty()) {
            double totalSeconds = 0;
            int validSamples = 0;
            for (Object[] row : resolvedTimestamps) {
                Instant createdAt = (Instant) row[0];
                Instant resolvedAt = (Instant) row[1];
                long seconds = Duration.between(createdAt, resolvedAt).getSeconds();
                if (seconds < 0) {
                    continue;
                }
                totalSeconds += seconds;
                validSamples++;
            }
            if (validSamples > 0) {
                avgHandlingTimeSeconds = totalSeconds / validSamples;
            }
        }

        return new ReportMetrics(byStatus, avgHandlingTimeSeconds);
    }

    private List<TrendingTopic> buildTrendingTopics(Instant from, Instant to) {
        List<Object[]> rows = communityQuestionRepository.findTrendingTopics(
                from, to, PageRequest.of(0, TRENDING_TOP_N));
        return rows.stream()
                .map(row -> new TrendingTopic((java.util.UUID) row[0], (String) row[1], (Long) row[2]))
                .toList();
    }

    private Map<String, Long> groupCountsToMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] == null ? "UNASSIGNED" : row[0].toString();
            long count = ((Number) row[1]).longValue();
            result.put(key, count);
        }
        return result;
    }
}
