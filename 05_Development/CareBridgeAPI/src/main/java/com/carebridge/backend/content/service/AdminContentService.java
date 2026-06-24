package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;

public interface AdminContentService {

    CreateContentResponse createContent(CreateContentRequest request, java.util.UUID authorUserId);
}
