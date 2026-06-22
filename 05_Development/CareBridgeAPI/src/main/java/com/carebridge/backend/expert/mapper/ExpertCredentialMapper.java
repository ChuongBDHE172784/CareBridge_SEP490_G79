package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ExpertCredentialResponse;
import com.carebridge.backend.expert.entity.ExpertCredential;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExpertCredentialMapper {

    ExpertCredentialMapper INSTANCE = Mappers.getMapper(ExpertCredentialMapper.class);

    ExpertCredentialResponse toResponse(ExpertCredential entity);

    List<ExpertCredentialResponse> toResponseList(List<ExpertCredential> entities);
}
