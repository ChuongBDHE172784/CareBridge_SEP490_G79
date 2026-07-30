package com.carebridge.backend.masterdata.service;

import com.carebridge.backend.masterdata.dto.response.ProvinceResponse;
import com.carebridge.backend.masterdata.dto.response.WardResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProvinceCacheService {

    private static final String PROVINCE_API_URL = "https://provinces.open-api.vn/api/v2/?depth=2";
    private static final long CACHE_TTL_HOURS = 24;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // In-memory cache
    private List<ProvinceResponse> provincesCache = new ArrayList<>();
    private final Map<String, List<WardResponse>> wardsByProvinceCache = new ConcurrentHashMap<>();
    private Instant lastFetchTime = null;

    public ProvinceCacheService() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public synchronized void refreshCacheIfNeeded() {
        if (lastFetchTime != null && Duration.between(lastFetchTime, Instant.now()).toHours() < CACHE_TTL_HOURS) {
            return;
        }

        try {
            log.info("Fetching province data from Open API...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROVINCE_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> rawProvinces = objectMapper.readValue(
                        response.body(), new TypeReference<>() {});
                
                List<ProvinceResponse> parsedProvinces = new ArrayList<>();
                Map<String, List<WardResponse>> parsedWards = new ConcurrentHashMap<>();

                for (Map<String, Object> p : rawProvinces) {
                    String pCode = String.valueOf(p.get("code"));
                    String pName = (String) p.get("name");
                    parsedProvinces.add(ProvinceResponse.builder()
                            .provinceId(pCode)
                            .name(pName)
                            .build());

                    List<WardResponse> wardsForProvince = new ArrayList<>();
                    
                    if (p.get("wards") instanceof List wards) {
                        for (Object wObj : wards) {
                            if (wObj instanceof Map wMap) {
                                String wCode = String.valueOf(wMap.get("code"));
                                String wName = (String) wMap.get("name");
                                wardsForProvince.add(WardResponse.builder()
                                        .wardId(wCode)
                                        .name(wName)
                                        .provinceId(pCode)
                                        .districtId(null) // No district in v2 direct mapping
                                        .build());
                            }
                        }
                    }
                    parsedWards.put(pCode, wardsForProvince);
                }

                this.provincesCache = parsedProvinces;
                this.wardsByProvinceCache.clear();
                this.wardsByProvinceCache.putAll(parsedWards);
                this.lastFetchTime = Instant.now();
                log.info("Successfully cached {} provinces.", parsedProvinces.size());
            } else {
                log.error("Failed to fetch province data: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Exception fetching province data", e);
        }
    }

    public List<ProvinceResponse> getProvinces() {
        refreshCacheIfNeeded();
        return Collections.unmodifiableList(provincesCache);
    }

    public List<WardResponse> getWardsByProvince(String provinceId) {
        refreshCacheIfNeeded();
        return wardsByProvinceCache.getOrDefault(provinceId, Collections.emptyList());
    }
}
