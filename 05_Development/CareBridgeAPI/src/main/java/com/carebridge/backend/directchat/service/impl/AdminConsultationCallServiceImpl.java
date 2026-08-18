package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.directchat.dto.request.AdminConsultationCallSearchQuery;
import com.carebridge.backend.directchat.dto.response.AdminConsultationCallSummaryResponse;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.service.IAdminConsultationCallService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminConsultationCallServiceImpl implements IAdminConsultationCallService {

    private final ConversationCallRepository callRepository;
    private final DirectConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final IFileService fileService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminConsultationCallSummaryResponse> searchCalls(AdminConsultationCallSearchQuery query, Pageable pageable) {
        Specification<ConversationCall> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getCallType() != null) {
                predicates.add(cb.equal(root.get("callType"), query.getCallType()));
            }
            if (query.getCallStatus() != null) {
                predicates.add(cb.equal(root.get("callStatus"), query.getCallStatus()));
            }
            if (query.getHasRecording() != null) {
                if (Boolean.TRUE.equals(query.getHasRecording())) {
                    predicates.add(cb.isNotNull(root.get("recordingFileId")));
                } else {
                    predicates.add(cb.isNull(root.get("recordingFileId")));
                }
            }
            if (query.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("initiatedAt"), query.getFromDate()));
            }
            if (query.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("initiatedAt"), query.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ConversationCall> callPage = callRepository.findAll(spec, pageable);

        if (callPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Bulk load conversations
        Set<UUID> conversationIds = callPage.getContent().stream()
                .map(ConversationCall::getConversationId)
                .collect(Collectors.toSet());
        Map<UUID, DirectConversation> conversations = conversationRepository.findAllById(conversationIds).stream()
                .collect(Collectors.toMap(DirectConversation::getId, c -> c));

        // Bulk load users (mothers, experts, callers)
        Set<UUID> userIds = conversations.values().stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getMotherUserId(), c.getExpertUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        callPage.getContent().forEach(c -> userIds.add(c.getInitiatedByUserId()));

        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Bulk load expert profiles
        Set<UUID> expertUserIds = conversations.values().stream()
                .map(DirectConversation::getExpertUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ExpertProfile> expertProfiles = expertProfileRepository.findByUserIdIn(expertUserIds).stream()
                .collect(Collectors.toMap(ExpertProfile::getExpertProfileId, ep -> ep));

        List<AdminConsultationCallSummaryResponse> dtos = callPage.getContent().stream()
                .map(call -> mapToAdminSummary(call, conversations.get(call.getConversationId()), users, expertProfiles))
                .filter(dto -> matchesKeyword(dto, query.getKeyword()))
                .toList();

        return new PageImpl<>(dtos, pageable, callPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminConsultationCallSummaryResponse getCallDetail(UUID callId) {
        ConversationCall call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));
        DirectConversation conversation = conversationRepository.findById(call.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + call.getConversationId()));

        Set<UUID> userIds = java.util.stream.Stream.of(
                conversation.getMotherUserId(), conversation.getExpertUserId(), call.getInitiatedByUserId()
        ).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, ExpertProfile> expertProfiles = expertProfileRepository.findByUserId(conversation.getExpertUserId())
                .map(ep -> Map.of(ep.getExpertProfileId(), ep))
                .orElse(Map.of());

        return mapToAdminSummary(call, conversation, users, expertProfiles);
    }

    @Override
    @Transactional(readOnly = true)
    public String getRecordingPresignedUrl(UUID callId, UUID adminUserId) {
        ConversationCall call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        if (call.getRecordingFileId() == null) {
            throw new ResourceNotFoundException("No recording file associated with this call");
        }

        // Generate pre-signed URL valid for 60 minutes for streaming playback
        return fileService.generatePresignedUrl(call.getRecordingFileId(), adminUserId, 60);
    }

    private AdminConsultationCallSummaryResponse mapToAdminSummary(
            ConversationCall call,
            DirectConversation conversation,
            Map<UUID, User> users,
            Map<UUID, ExpertProfile> expertProfiles) {

        UUID motherId = conversation != null ? conversation.getMotherUserId() : null;
        UUID expertId = conversation != null ? conversation.getExpertUserId() : null;

        User motherUser = motherId != null ? users.get(motherId) : null;
        User expertUser = expertId != null ? users.get(expertId) : null;
        ExpertProfile expertProfile = expertId != null ? expertProfiles.get(expertId) : null;

        String callerRole = "UNKNOWN";
        if (call.getInitiatedByUserId().equals(motherId)) {
            callerRole = "MOTHER";
        } else if (call.getInitiatedByUserId().equals(expertId)) {
            callerRole = "EXPERT";
        }

        String motherName = motherUser != null ? (motherUser.getDisplayName() != null ? motherUser.getDisplayName() : motherUser.getName()) : "Người mẹ";
        String motherPhone = motherUser != null ? motherUser.getPhone() : null;
        String motherEmail = motherUser != null ? motherUser.getEmail() : null;

        String expertName = expertUser != null ? (expertUser.getDisplayName() != null ? expertUser.getDisplayName() : expertUser.getName()) : "Bác sĩ / Chuyên gia";
        String expertSpecialization = expertProfile != null ? expertProfile.getSpecialty() : null;
        String expertHospital = expertProfile != null ? expertProfile.getWorkplace() : null;

        return AdminConsultationCallSummaryResponse.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .callType(call.getCallType().name())
                .callStatus(call.getCallStatus().name())
                .initiatedAt(call.getInitiatedAt())
                .answeredAt(call.getAnsweredAt())
                .endedAt(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .recordingFileId(call.getRecordingFileId())
                .recordingStatus(call.getRecordingStatus())
                .recordedDurationSeconds(call.getRecordedDurationSeconds())
                .consentAttested(call.isConsentAttested())
                .initiatedByUserId(call.getInitiatedByUserId())
                .initiatedByRole(callerRole)
                .motherUserId(motherId)
                .motherName(motherName)
                .motherPhone(motherPhone)
                .motherEmail(motherEmail)
                .expertUserId(expertId)
                .expertName(expertName)
                .expertSpecialization(expertSpecialization)
                .expertHospital(expertHospital)
                .build();
    }

    private boolean matchesKeyword(AdminConsultationCallSummaryResponse dto, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.toLowerCase().trim();
        return (dto.getMotherName() != null && dto.getMotherName().toLowerCase().contains(kw))
                || (dto.getExpertName() != null && dto.getExpertName().toLowerCase().contains(kw))
                || (dto.getMotherPhone() != null && dto.getMotherPhone().contains(kw))
                || (dto.getMotherEmail() != null && dto.getMotherEmail().toLowerCase().contains(kw));
    }
}
