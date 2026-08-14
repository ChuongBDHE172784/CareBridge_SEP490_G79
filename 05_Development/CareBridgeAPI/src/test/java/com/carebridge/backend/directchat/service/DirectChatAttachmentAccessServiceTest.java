package com.carebridge.backend.directchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.entity.MessageType;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.service.IFileService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DirectChatAttachmentAccessServiceTest {

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID FILE_ID = UUID.randomUUID();

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private IDirectConversationPolicy policy;
    @Mock private IFileService fileService;

    private DirectChatAttachmentAccessService service;

    @BeforeEach
    void setUp() {
        service = new DirectChatAttachmentAccessService(
                conversationRepository, messageRepository, policy, fileService);
    }

    @Test
    void familyParticipant_canViewAttachmentUsingSenderAuthority() {
        DirectConversation conversation = conversation();
        DirectMessage message = DirectMessage.builder()
                .id(MESSAGE_ID).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .messageType(MessageType.IMAGE).attachmentId(FILE_ID).createdAt(Instant.now()).build();
        ViewFileResponse response = ViewFileResponse.builder().fileId(FILE_ID).presignedUrl("https://signed").build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByIdAndConversationId(MESSAGE_ID, CONVERSATION_ID)).thenReturn(Optional.of(message));
        when(fileService.viewFile(FILE_ID, MOTHER_ID)).thenReturn(response);

        ViewFileResponse actual = service.view(CONVERSATION_ID, MESSAGE_ID, FAMILY_ID);

        assertThat(actual.getPresignedUrl()).isEqualTo("https://signed");
        verify(policy).assertIsParticipant(FAMILY_ID, conversation);
        verify(fileService).viewFile(FILE_ID, MOTHER_ID);
    }

    @Test
    void recalledAttachment_neverIssuesAnotherDownloadUrl() {
        DirectConversation conversation = conversation();
        DirectMessage recalled = DirectMessage.builder()
                .id(MESSAGE_ID).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .messageType(MessageType.FILE).attachmentId(FILE_ID).recalledAt(Instant.now()).createdAt(Instant.now()).build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByIdAndConversationId(MESSAGE_ID, CONVERSATION_ID)).thenReturn(Optional.of(recalled));

        assertThatThrownBy(() -> service.view(CONVERSATION_ID, MESSAGE_ID, EXPERT_ID))
                .isInstanceOf(DirectChatException.class);

        verify(fileService, never()).viewFile(any(), any());
    }

    @Test
    void participantUpload_isAlwaysPrivateAndServerAssignedPurpose() {
        DirectConversation conversation = conversation();
        MockMultipartFile image = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1, 2});
        UploadFileResponse response = UploadFileResponse.builder().fileId(FILE_ID).build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(fileService.detectKind(image)).thenReturn(FileKind.IMAGE);
        when(fileService.uploadWithPurpose(any(), any(), any(), any(), any())).thenReturn(response);

        UploadFileResponse actual = service.upload(CONVERSATION_ID, FAMILY_ID, image);

        assertThat(actual.getFileId()).isEqualTo(FILE_ID);
        verify(policy).assertIsParticipant(FAMILY_ID, conversation);
        verify(fileService).uploadWithPurpose(
                image, FAMILY_ID, FileKind.IMAGE,
                com.carebridge.backend.file.enums.FilePurpose.DIRECT_CHAT_IMAGE,
                com.carebridge.backend.file.enums.FileAccessMode.PRIVATE);
    }

    @Test
    void participantUpload_routesDocumentsByContentNotByCallerHint() {
        DirectConversation conversation = conversation();
        // Named like an image on purpose: the routing must follow the bytes, not the name.
        MockMultipartFile doc = new MockMultipartFile("file", "report.jpg", "image/jpeg", new byte[] {1, 2});
        UploadFileResponse response = UploadFileResponse.builder().fileId(FILE_ID).build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(fileService.detectKind(doc)).thenReturn(FileKind.DOCUMENT);
        when(fileService.uploadWithPurpose(any(), any(), any(), any(), any())).thenReturn(response);

        service.upload(CONVERSATION_ID, FAMILY_ID, doc);

        verify(fileService).uploadWithPurpose(
                doc, FAMILY_ID, FileKind.DOCUMENT,
                com.carebridge.backend.file.enums.FilePurpose.DIRECT_CHAT_DOCUMENT,
                com.carebridge.backend.file.enums.FileAccessMode.PRIVATE);
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder().id(CONVERSATION_ID)
                .motherUserId(MOTHER_ID).expertUserId(EXPERT_ID).status("ACTIVE").build();
    }
}
