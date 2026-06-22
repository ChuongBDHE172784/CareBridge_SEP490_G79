package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.AvailabilitySlotResponse;
import com.carebridge.backend.expert.entity.ExpertAvailability;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AvailabilityMapper {

    AvailabilityMapper INSTANCE = Mappers.getMapper(AvailabilityMapper.class);

    AvailabilitySlotResponse toResponse(ExpertAvailability entity);

    List<AvailabilitySlotResponse> toResponseList(List<ExpertAvailability> entities);
}
