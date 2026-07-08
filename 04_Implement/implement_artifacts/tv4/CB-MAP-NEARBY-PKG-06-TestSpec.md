# CB-MAP-NEARBY-PKG-06-TestSpec — Map + Nearby Care Tests

| Field | Value |
|-------|-------|
| **Document ID** | CB-MAP-NEARBY-PKG-06-TestSpec |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Spec gốc** | CB-MAP-NEARBY-PKG-06-TDS |
| **Package** | PKG-06 — Map & Nearby Care |
| **Included UCs** | UC-77, UC-78, UC-79, UC-80, UC-81, UC-82 |

---

## Test Design

### Conditions

| ID | Description |
|----|-------------|
| TC-COND-601 | Search nearby facilities by lat/lng/radius |
| TC-COND-602 | Missing location params → 400 |
| TC-COND-603 | Facility not found → 404 |
| TC-COND-604 | Emergency handoff created by triage result |
| TC-COND-605 | Non-mother cannot access emergency |
| TC-COND-606 | Route request returns stub ETA |
| TC-COND-607 | Invalid coordinates → 400 |

---

## Test Cases

### TC-601 — Nearby Facilities Search

```java
@Test
void nearbyFacilities_withValidCoords_returnsList() {
    BigDecimal lat = new BigDecimal("10.0186");
    BigDecimal lng = new BigDecimal("105.7878");

    when(facilityRepository.findNearby(any(), any(), anyInt()))
            .thenReturn(List.of(seedHospital()));

    NearbyResponse result = service.searchNearby(lat, lng, 5000, null);

    assertThat(result.facilities()).hasSize(1);
    assertThat(result.facilities().get(0).facilityType()).isEqualTo("HOSPITAL");
}
```

### TC-602 — Missing Params → 400

```java
@Test
void nearbyFacilities_withoutLat_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/map/nearby-facilities")
            .param("lng", "105.78"))
            .andExpect(status().isBadRequest());
}
```

### TC-603 — Facility Not Found → 404

```java
@Test
void getFacility_withInvalidId_returns404() throws Exception {
    mockMvc.perform(get("/api/v1/map/facilities/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound());
}
```

### TC-604 — Emergency Handoff Created

```java
@Test
void createEmergencyHandoff_withValidTriage_returnsHandoff() {
    TriageResultHandoff handoff = makeTriageHandoff(b -> b
            .triageHandoffId(UUID.randomUUID())
            .riskLevel("RED")
            .userLatitude(new BigDecimal("10.0186"))
            .userLongitude(new BigDecimal("105.7878")));
    when(triagePort.readHandoff(any())).thenReturn(handoff);
    when(handoffRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    EmergencyHandoffResponse result = service.createEmergencyHandoff("uuid", USER_ID);

    assertThat(result.riskLevel()).isEqualTo("RED");
    assertThat(result.status()).isEqualTo("OPEN");
}
```

### TC-605 — Non-Mother Cannot Access Emergency

```java
@Test
@WithMockUser(roles = {"EXPERT"})
void getEmergency_withExpertRole_returns403() throws Exception {
    mockMvc.perform(get("/api/v1/map/emergency/{id}", UUID.randomUUID()))
            .andExpect(status().isForbidden());
}
```

### TC-606 — Route Stub Returns ETA

```java
@Test
void getRoute_withStubProvider_returnsEta() {
    RouteRequest req = makeRouteReq(b -> b
            .fromLat(new BigDecimal("10.0186"))
            .fromLng(new BigDecimal("105.7878"))
            .toLat(new BigDecimal("10.0156"))
            .toLng(new BigDecimal("105.7867")));

    RouteResponse result = routeProvider.getRoute(req);

    assertThat(result.etaMinutes()).isNotNull();
    assertThat(result.etaMinutes()).isGreaterThan(0);
}
```

### TC-607 — Invalid Coordinates → 400

```java
@Test
void nearbyFacilities_withLat100_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/map/nearby-facilities")
            .param("lat", "100")
            .param("lng", "105.78"))
            .andExpect(status().isBadRequest());
}
```

---

## Commands

```bash
./mvnw test -Dtest=CareFacility*,EmergencyMap*,LocationSnapshot*,RouteProvider*
```

---

*CareBridge Test-Spec v1.0*
