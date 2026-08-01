package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.dto.response.UnreadSummaryResponse;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.LastMessageRow;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectConversationServiceImplSummaryTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private ConversationSummaryAggregateRepository aggregateRepository;
    @Mock private IDirectConversationPolicy policy;
    @Mock private DirectConversationWriter writer;
    @Mock private AuditService auditService;

    private DirectConversationServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-15T08:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_USER_ID = UUID.randomUUID();
    private static final UUID FAMILY_USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DirectConversationServiceImpl(
                conversationRepository, expertProfileRepository, userRepository, messageRepository,
                aggregateRepository, policy, writer, auditService, fixedClock);
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder()
                .id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                .status("ACTIVE").lastActivityAt(Instant.parse("2026-07-15T09:00:00Z")).build();
    }

    private static ExpertProfile approvedExpert() {
        return ExpertProfile.builder().userId(EXPERT_USER_ID).specialty("Sản khoa")
                .verificationStatus(VerificationStatus.APPROVED).build();
    }

    // MEDI-TC-008 — counterpart name/avatar/specialty, both viewer directions
    @Test
    void listMyConversations_motherViewer_seesExpertCounterpartWithSpecialty() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID))
                .thenReturn(List.of(conversation()));
        User expertUser = User.builder().id(EXPERT_USER_ID).name("BS. Nguyễn Văn A").avatarUrl("https://x/e.jpg").build();
        when(userRepository.findAllById(anySet())).thenReturn(List.of(expertUser));
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(MOTHER_ID))).thenReturn(Map.of());

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(MOTHER_ID);

        DirectConversationSummaryResponse s = summaries.get(0);
        assertThat(s.getCounterpartRole()).isEqualTo("EXPERT");
        assertThat(s.getCounterpartDisplayName()).isEqualTo("BS. Nguyễn Văn A");
        assertThat(s.getCounterpartAvatarUrl()).isEqualTo("https://x/e.jpg");
        assertThat(s.getCounterpartSpecialty()).isEqualTo("Sản khoa");
    }

    @Test
    void listMyConversations_expertViewer_seesMotherCounterpartWithNullSpecialty() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(EXPERT_USER_ID, EXPERT_USER_ID))
                .thenReturn(List.of(conversation()));
        User motherUser = User.builder().id(MOTHER_ID).name("Trần Thị B").build();
        when(userRepository.findAllById(anySet())).thenReturn(List.of(motherUser));
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(EXPERT_USER_ID))).thenReturn(Map.of());

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(EXPERT_USER_ID);

        DirectConversationSummaryResponse s = summaries.get(0);
        assertThat(s.getCounterpartRole()).isEqualTo("MOTHER");
        assertThat(s.getCounterpartDisplayName()).isEqualTo("Trần Thị B");
        assertThat(s.getCounterpartSpecialty()).isNull();
    }

    @Test
    void listMyConversations_familyViewer_seesDelegatedConversationWithExpertCounterpart() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(FAMILY_USER_ID, FAMILY_USER_ID))
                .thenReturn(List.of());
        when(conversationRepository.findDelegatedToFamilyUserOrderByLastActivityAtDesc(FAMILY_USER_ID))
                .thenReturn(List.of(conversation()));
        User expertUser = User.builder().id(EXPERT_USER_ID).name("BS. Nguyễn Văn A").build();
        when(userRepository.findAllById(anySet())).thenReturn(List.of(expertUser));
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(FAMILY_USER_ID))).thenReturn(Map.of());

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(FAMILY_USER_ID);

        assertThat(summaries).singleElement().satisfies(summary -> {
            assertThat(summary.getCounterpartRole()).isEqualTo("EXPERT");
            assertThat(summary.getCounterpartUserId()).isEqualTo(EXPERT_USER_ID);
        });
    }

    // MEDI-TC-010 — lastMessagePreview / lastMessageAt come from the aggregate query
    @Test
    void listMyConversations_populatesLastMessagePreviewAndTimestamp() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID))
                .thenReturn(List.of(conversation()));
        when(userRepository.findAllById(anySet())).thenReturn(List.of());
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        Instant lastAt = Instant.parse("2026-07-15T09:30:00Z");
        when(aggregateRepository.fetchLastMessages(any()))
                .thenReturn(Map.of(CONVERSATION_ID, new LastMessageRow(UUID.randomUUID(), "Xin chào bác sĩ", lastAt)));
        when(aggregateRepository.fetchUnreadCounts(any(), eq(MOTHER_ID))).thenReturn(Map.of());

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(MOTHER_ID);

        assertThat(summaries.get(0).getLastMessagePreview()).isEqualTo("Xin chào bác sĩ");
        assertThat(summaries.get(0).getLastMessageAt()).isEqualTo(lastAt);
    }

    @Test
    void listMyConversations_noMessageYet_previewIsNull() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID))
                .thenReturn(List.of(conversation()));
        when(userRepository.findAllById(anySet())).thenReturn(List.of());
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(MOTHER_ID))).thenReturn(Map.of());

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(MOTHER_ID);

        assertThat(summaries.get(0).getLastMessagePreview()).isNull();
        assertThat(summaries.get(0).getLastMessageAt()).isNull();
    }

    // MEDI-TC-011 — unreadCount comes straight from the aggregate map, per conversation
    @Test
    void listMyConversations_unreadCountFromAggregateMap() {
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(EXPERT_USER_ID, EXPERT_USER_ID))
                .thenReturn(List.of(conversation()));
        when(userRepository.findAllById(anySet())).thenReturn(List.of());
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(EXPERT_USER_ID)))
                .thenReturn(Map.of(CONVERSATION_ID, 1));

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(EXPERT_USER_ID);

        assertThat(summaries.get(0).getUnreadCount()).isEqualTo(1);
    }

    // MEDI-TC-017 — exactly 1 call per repository method, regardless of how many conversations
    @Test
    void listMyConversations_queryCountFixedRegardlessOfN() {
        List<DirectConversation> tenConversations = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> DirectConversation.builder()
                        .id(UUID.randomUUID()).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                        .status("ACTIVE").lastActivityAt(fixedNow).build())
                .toList();
        when(conversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID))
                .thenReturn(tenConversations);
        when(userRepository.findAllById(anySet())).thenReturn(List.of());
        when(expertProfileRepository.findByUserIdIn(anySet())).thenReturn(List.of(approvedExpert()));
        when(aggregateRepository.fetchLastMessages(any())).thenReturn(Map.of());
        when(aggregateRepository.fetchUnreadCounts(any(), eq(MOTHER_ID))).thenReturn(Map.of());

        service.listMyConversations(MOTHER_ID);

        verify(conversationRepository, times(1))
                .findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(MOTHER_ID, MOTHER_ID);
        verify(userRepository, times(1)).findAllById(anySet());
        verify(expertProfileRepository, times(1)).findByUserIdIn(anySet());
        verify(aggregateRepository, times(1)).fetchLastMessages(any());
        verify(aggregateRepository, times(1)).fetchUnreadCounts(any(), eq(MOTHER_ID));
        verify(expertProfileRepository, never()).findByUserId(any());
    }

    // MEDI-TC-018 — unreadConversationCount counts conversations with unread>0, totalUnreadMessageCount sums all
    @Test
    void getUnreadSummary_distinguishesConversationCountFromMessageCount() {
        UUID convA = UUID.randomUUID();
        UUID convB = UUID.randomUUID();
        UUID convC = UUID.randomUUID();
        DirectConversation a = DirectConversation.builder().id(convA).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID).status("ACTIVE").build();
        DirectConversation b = DirectConversation.builder().id(convB).motherUserId(MOTHER_ID).expertUserId(UUID.randomUUID()).status("ACTIVE").build();
        DirectConversation c = DirectConversation.builder().id(convC).motherUserId(MOTHER_ID).expertUserId(UUID.randomUUID()).status("ACTIVE").build();
        when(conversationRepository.findByMotherUserIdOrExpertUserId(MOTHER_ID, MOTHER_ID)).thenReturn(List.of(a, b, c));
        when(aggregateRepository.fetchUnreadCounts(any(), eq(MOTHER_ID)))
                .thenReturn(Map.of(convA, 5, convB, 1, convC, 0));

        UnreadSummaryResponse summary = service.getUnreadSummary(MOTHER_ID);

        assertThat(summary.unreadConversationCount()).isEqualTo(2);
        assertThat(summary.totalUnreadMessageCount()).isEqualTo(6);
    }

    @Test
    void getUnreadSummary_noConversations_returnsZeros() {
        when(conversationRepository.findByMotherUserIdOrExpertUserId(MOTHER_ID, MOTHER_ID)).thenReturn(List.of());

        UnreadSummaryResponse summary = service.getUnreadSummary(MOTHER_ID);

        assertThat(summary.unreadConversationCount()).isEqualTo(0);
        assertThat(summary.totalUnreadMessageCount()).isEqualTo(0);
        verify(aggregateRepository, never()).fetchUnreadCounts(any(), any());
    }
}
