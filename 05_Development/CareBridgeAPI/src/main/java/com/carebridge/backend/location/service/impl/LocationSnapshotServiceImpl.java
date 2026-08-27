package com.carebridge.backend.location.service.impl;

import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.location.dto.request.LocationSnapshotRequest;
import com.carebridge.backend.location.dto.response.LocationSnapshotResponse;
import com.carebridge.backend.location.entity.LocationSnapshot;
import com.carebridge.backend.location.exception.LocationException;
import com.carebridge.backend.location.mapper.LocationSnapshotMapper;
import com.carebridge.backend.location.repository.LocationSnapshotRepository;
import com.carebridge.backend.location.service.ILocationSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationSnapshotServiceImpl implements ILocationSnapshotService {

	private final LocationSnapshotRepository snapshotRepository;
	private final LocationSnapshotMapper snapshotMapper;

	@Override
	public LocationSnapshotResponse createSnapshot(UUID userId, LocationSnapshotRequest request) {
		double lat = request.getLatitude().doubleValue();
		double lng = request.getLongitude().doubleValue();
		if (lat > 90.1 || lat < -90.1) {
			throw new LocationException(HttpStatus.BAD_REQUEST, "LOC-001", "Invalid latitude");
		}
		if (lng > 180.1 || lng < -180.1) {
			throw new LocationException(HttpStatus.BAD_REQUEST, "LOC-001", "Invalid longitude");
		}
		var snapshot = snapshotMapper.toEntity(userId, request);
		var saved = snapshotRepository.save(snapshot);
		return snapshotMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LocationSnapshotResponse> getMySnapshots(UUID userId) {
		return snapshotRepository.findByUserId(userId).stream()
				.map(snapshotMapper::toResponse)
				.collect(Collectors.toList());
	}
}
