package com.carebridge.backend.health.device.service;

import com.carebridge.backend.health.device.dto.ImportDeviceMetricRequest;
import com.carebridge.backend.health.device.dto.ImportDeviceMetricResponse;
import java.util.UUID;

public interface IDeviceDataImportService {

    ImportDeviceMetricResponse importMetric(ImportDeviceMetricRequest request, UUID userId);
}
