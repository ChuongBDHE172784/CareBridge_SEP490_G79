package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExpertProfileMapper {

    ExpertProfileMapper INSTANCE = Mappers.getMapper(ExpertProfileMapper.class);

    ExpertProfile toEntity(CreateExpertProfileRequest request);

    ExpertProfile toEntity(UpdateExpertProfileRequest request, @MappingTarget ExpertProfile entity);

    ExpertProfileResponse toResponse(ExpertProfile entity);

    ExpertProfilePublicResponse toPublicResponse(ExpertProfile entity);

    List<ExpertProfilePublicResponse> toPublicResponseList(List<ExpertProfile> entities);
}
