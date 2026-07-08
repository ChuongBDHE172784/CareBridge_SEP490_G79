package com.carebridge.backend.location.service;

import com.carebridge.backend.location.dto.request.LocationSnapshotRequest;
import com.carebridge.backend.location.dto.response.LocationSnapshotResponse;
import java.util.List;
import java.util.UUID;

public interface ILocationSnapshotService {

    LocationSnapshotResponse createSnapshot(UUID userId, LocationSnapshotRequest request);

    List<LocationSnapshotResponse> getMySnapshots(UUID userId);
}
