package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.service.IFileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Participant-scoped file access. Keeps generic file policy unchanged. */
@Service
@RequiredArgsConstructor
public class DirectChatAttachmentAccessService {
    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final IDirectConversationPolicy policy;
    private final IFileService fileService;

    /**
     * Uploads media only after confirming the caller belongs to this direct conversation.
     * The purpose and private access mode are server-selected so callers cannot turn chat
     * media into public content by altering multipart parameters.
     */
    @Transactional
    public UploadFileResponse upload(UUID conversationId, UUID currentUserId, MultipartFile file, FileKind kind) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);
        FilePurpose purpose = kind == FileKind.IMAGE
                ? FilePurpose.DIRECT_CHAT_IMAGE
                : FilePurpose.DIRECT_CHAT_DOCUMENT;
        return fileService.uploadWithPurpose(file, currentUserId, kind, purpose, FileAccessMode.PRIVATE);
    }

    @Transactional(readOnly = true)
    public ViewFileResponse view(UUID conversationId, UUID messageId, UUID currentUserId) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);
        DirectMessage message = messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(DirectChatException::idempotencyConflict);
        if (message.getAttachmentId() == null || message.getRecalledAt() != null) {
            throw DirectChatException.idempotencyConflict();
        }
        return fileService.viewFile(message.getAttachmentId(), message.getSenderUserId());
    }
}
