# CB-EXP-AVAIL-LOC-PKG-02-TestSpec — Expert Availability + Location Share Tests

| Field | Value |
|-------|-------|
| **Document ID** | CB-EXP-AVAIL-LOC-PKG-02-TestSpec |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Spec gốc** | CB-EXP-AVAIL-LOC-PKG-02-TDS |
| **Package** | PKG-02 — Expert Availability & Location |
| **Included UCs** | UC-63, UC-64, UC-78, UC-79 |

---

## Test Design

### Scope
- Service Unit Tests (Mockito)
- Controller Tests (MockMvc, auth 401/403)
- No integration tests (no complex DB constraints)

### Conditions

| ID | Description | TC |
|----|-------------|-----|
| TC-COND-101 | Create availability window (happy) | TC-101 |
| TC-COND-102 | endAt before startAt → 400 | TC-102 |
| TC-COND-103 | Wrong role → 403 | TC-103 |
| TC-COND-104 | Unauthenticated → 401 | TC-104 |
| TC-COND-105 | Share location with coords | TC-105 |
| TC-COND-106 | Invalid coords → 400 | TC-106 |
| TC-COND-107 | Location with consent → 403 if no consent | TC-107 |
| TC-COND-108 | Stop sharing location (delete) | TC-108 |

---

## Test Cases

### TC-101 — Create Availability Window

**Severity:** HIGH
```java
@Test
void createAvailability_withValidWindow_returnsResponse() {
    CreateAvailabilityRequest req = makeAvailabilityReq(b -> b
            .startAt(Instant.parse("2026-07-04T09:00:00Z"))
            .endAt(Instant.parse("2026-07-04T17:00:00Z"))
            .channelType("ONLINE_CHAT")
            .status("AVAILABLE"));
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    AvailabilityResponse result = service.createAvailability(EXPERT_USER, req);

    assertThat(result.status()).isEqualTo("AVAILABLE");
}
```

### TC-102 — endAt Before startAt → 400

**Oracle:** ADR-AVAIL-001 c3
```java
@Test
void createAvailability_whenEndBeforeStart_throwsBadRequest() {
    CreateAvailabilityRequest req = makeAvailabilityReq(b -> b
            .startAt(Instant.parse("2026-07-04T17:00:00Z"))
            .endAt(Instant.parse("2026-07-04T09:00:00Z")));

    assertThatThrownBy(() -> service.createAvailability(EXPERT_USER, req))
            .isInstanceOf(ExpertException.class)
            .satisfies(e -> assertThat(((ExpertException) e).getCode()).isEqualTo("EXPERT-011"));
}
```

### TC-103 — Wrong Role → 403 on availability create

```java
@Test
@WithMockUser(roles = {"MOTHER"})
void createAvailability_withMotherRole_returns403() throws Exception {
    mockMvc.perform(post("/api/v1/expert/availability")
            .contentType(APPLICATION_JSON)
            .content("{\"startAt\":\"2026-07-04T09:00:00Z\",\"endAt\":\"2026-07-04T17:00:00Z\"}"))
            .andExpect(status().isForbidden());
}
```

### TC-104 — Unauthenticated → 401

```java
@Test
void createAvailability_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/expert/availability")
            .contentType(APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized());
}
```

### TC-105 — Share Location Valid

```java
@Test
void shareLocation_withValidData_returnsResponse() {
    ShareLocationRequest req = makeLocationReq(b -> b
            .latitude(new BigDecimal("10.8231"))
            .longitude(new BigDecimal("106.6297"))
            .accuracyMeters(new BigDecimal("15")));
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    LocationShareResponse result = service.shareLocation(EXPERT_USER, req);

    assertThat(result.latitude()).isEqualByComparingTo(new BigDecimal("10.8231"));
}
```

### TC-106 — Invalid Latitude → 400

```java
@Test
void shareLocation_withLatitude100_throwsBadRequest() {
    ShareLocationRequest req = makeLocationReq(b -> b.latitude(new BigDecimal("100")));

    assertThatThrownBy(() -> service.shareLocation(EXPERT_USER, req))
            .isInstanceOf(ExpertException.class)
            .satisfies(e -> assertThat(((ExpertException) e).getCode()).isEqualTo("EXPERT-014"));
}
```

### TC-107 — Location without Consent → 403

```java
@Test
void shareLocation_withoutConsent_throwsForbidden() {
    ShareLocationRequest req = makeLocationReq(b -> b
            .consentReference(null));  // no consent

    assertThatThrownBy(() -> service.shareLocation(EXPERT_USER, req))
            .isInstanceOf(ExpertException.class)
            .satisfies(e -> assertThat(((ExpertException) e).getCode()).isEqualTo("EXPERT-013"));
}
```

### TC-108 — Stop Sharing Location → 200

```java
@Test
@WithMockUser(roles = {"EXPERT"})
void stopSharingLocation_returns204() throws Exception {
    doNothing().when(service).stopLocationShare(EXPERT_USER);

    mockMvc.perform(delete("/api/v1/expert/location/share"))
            .andExpect(status().isNoContent());
}
```

---

## Red-Green-Refactor

| TC | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|----|--------|----------|-------------|
| TC-101 | ☐ | — | — |
| TC-102 | ☐ | — | — |
| TC-103 | ☐ | — | — |
| TC-104 | ☐ | — | — |
| TC-105 | ☐ | — | — |
| TC-106 | ☐ | — | — |
| TC-107 | ☐ | — | — |
| TC-108 | ☐ | — | — |

```
./mvnw test -Dtest=ExpertAvailability*,ExpertLocation*
```

---

*CareBridge Test-Spec v1.0*
