package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ReviewResponse;
import com.carebridge.backend.expert.entity.ExpertReview;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExpertReviewMapper {

    ExpertReviewMapper INSTANCE = Mappers.getMapper(ExpertReviewMapper.class);

    ReviewResponse toResponse(ExpertReview entity);

    List<ReviewResponse> toResponseList(List<ExpertReview> entities);
}
