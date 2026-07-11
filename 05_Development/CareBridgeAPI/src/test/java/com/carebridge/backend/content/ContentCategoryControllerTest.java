package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.*;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.service.CommunityTopicService;
import com.carebridge.backend.content.controller.ContentCategoryController;
import java.security.Principal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentCategoryControllerTest {
    static final UUID ADMIN = UUID.fromString("f1800000-0000-0000-0000-0000000000ad");
    static final UUID TOPIC = UUID.fromString("f1900000-0000-0000-0000-000000000001");
    CommunityTopicService topics = mock(CommunityTopicService.class);
    AuditService audit = mock(AuditService.class);
    ContentCategoryController controller = new ContentCategoryController(topics, audit);
    Principal principal = () -> ADMIN.toString();
    CommunityTopicResponse response;

    @BeforeEach void setUp() { response = CommunityTopicResponse.builder().id(TOPIC).name("Nutrition").build(); }

    @Test void mccTc1101_listDelegates() {
        when(topics.getTopics(true, ADMIN)).thenReturn(List.of(response));
        assertEquals(1, Objects.requireNonNull(controller.listCategories(null, true, principal).getBody()).getData().size());
        verify(topics).getTopics(true, ADMIN);
    }

    @Test void mccTc1101_searchDelegates() {
        controller.listCategories("nutri", false, principal);
        verify(topics).searchTopics("nutri", false, ADMIN);
    }

    @Test void mccTc1102_createDelegatesAndAudits() {
        var request = CreateCommunityTopicRequest.builder().name("Nutrition").build();
        when(topics.createTopic(ADMIN, request)).thenReturn(response);
        assertEquals(201, controller.createCategory(request, principal).getStatusCode().value());
        verify(topics).createTopic(ADMIN, request);
        verify(audit).log(AuditAction.CONTENT_CATEGORY_MANAGED, ADMIN, "COMMUNITY_TOPIC", TOPIC.toString(), "action=CREATE");
    }

    @Test void mccTc1103_updateDelegatesAndAudits() {
        var request = UpdateCommunityTopicRequest.builder().name("Updated").build();
        when(topics.updateTopic(TOPIC, ADMIN, request)).thenReturn(response);
        controller.updateCategory(TOPIC, request, principal);
        verify(topics).updateTopic(TOPIC, ADMIN, request);
        verify(audit).log(AuditAction.CONTENT_CATEGORY_MANAGED, ADMIN, "COMMUNITY_TOPIC", TOPIC.toString(), "action=UPDATE");
    }
}
