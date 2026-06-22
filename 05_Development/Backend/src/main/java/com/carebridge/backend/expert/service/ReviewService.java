package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateReviewRequest;
import com.carebridge.backend.expert.dto.response.ReviewResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID motherId, CreateReviewRequest request);

    List<ReviewResponse> getReviewsByExpert(UUID expertId);
}
