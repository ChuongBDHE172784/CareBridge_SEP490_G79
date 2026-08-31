# MF-10 — Fall Detection Consent, Configuration, and Self-Test

| Field | Value |
| --- | --- |
| Major Feature | **MF-10 — Smart Activity Monitoring & Safety Support** |
| Function package | **Fall Detection Consent, Configuration, and Self-Test** |
| Code-first use cases | `UC-ES-04` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design consent/configuration, enable/disable, and sensor self-test state.

- **UC-ES-04 — Configure and Test Fall Detection:** Grant required consent/permissions, enable or disable fall detection, configure current safety settings, and complete sensor self-test.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-ES-04` | Configure and Test Fall Detection | `GET /api/v1/safety/config` | `SafetyConfigController.getConfig()` | `ISafetyConfigService.getConfig()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| `UC-ES-04` | Configure and Test Fall Detection | `PUT /api/v1/safety/config` | `SafetyConfigController.configure()` | `ISafetyConfigService.configure()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java` |
| `UC-ES-04` | Configure and Test Fall Detection | `POST /api/v1/safety/events/sensor-self-test` | `SensorSelfTestController.create()` | `SensorSelfTestService.create()` → `IImuMonitoringSessionRepository.findActiveForUpdateByUserId()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| `UC-ES-04` | Configure and Test Fall Detection | `POST /api/v1/safety/events/{eventId}/sensor-self-test/complete` | `SensorSelfTestController.complete()` | `SensorSelfTestService.complete()` → `ISafetyEventRepository.findLockedByIdAndUserId()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java` |
| `UC-ES-04` | Configure and Test Fall Detection | `POST /api/v1/safety/fall-detection/disable` | `FallDetectionController.disable()` | `IFallDetectionService.disable()` → `IImuMonitoringSessionRepository.acquireUserLock()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `UC-ES-04` | Configure and Test Fall Detection | `POST /api/v1/safety/fall-detection/enable` | `FallDetectionController.enable()` | `ISafetyConfigService.getConfig()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_01FallDetectionConsentConfigurationandSelfTest
skinparam classAttributeIconSize 0
skinparam wrapWidth 250
hide empty members

class "EnableFallDetectionScreen" as UIEnableFallDetectionScreen <<UI>>
class "SafetyConfigController" as ControllerSafetyConfigController <<Controller>> {
  - safetyConfigService: ISafetyConfigService
  + configure(request: SafetyConfigRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyConfigResponse>>
}
interface "ISafetyConfigService" as ServiceContractISafetyConfigService <<Service>> {
  + configure(request: SafetyConfigRequest, userId: UUID): SafetyConfigResponse
}
class "SafetyConfigService" as ServiceSafetyConfigService <<Service>> {
  - configStore: SafetyConfigStore
  - eventPublisher: ApplicationEventPublisher
  + configure(request: SafetyConfigRequest, userId: UUID): SafetyConfigResponse
}
ServiceContractISafetyConfigService <|.. ServiceSafetyConfigService : implements
UIEnableFallDetectionScreen ..> ControllerSafetyConfigController : invokes API
ControllerSafetyConfigController --> ServiceContractISafetyConfigService : delegates
@enduml
```

**Figure 1 — Class Diagram: Fall Detection Consent, Configuration, and Self-Test**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Fall Detection Consent, Configuration, and Self-Test — code-reachable representative flows

actor "Mother" as AMother
boundary "EnableFallDetectionScreen" as UIEnableFallDetectionScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "SafetyConfigController" as CSafetyConfigController <<control>>
participant "ISafetyConfigService" as SISafetyConfigService <<service>>

group UC-ES-04 — Configure and Test Fall Detection [configure()]
AMother -> UIEnableFallDetectionScreen : 1. submitFallDetectionConfiguration()
activate UIEnableFallDetectionScreen
alt [authorized request succeeds]
UIEnableFallDetectionScreen -> JWT : 2a. PUT /api/v1/safety/config with bearer token
activate JWT
JWT -> CSafetyConfigController : 2a-1. configure(request, principal)
activate CSafetyConfigController
CSafetyConfigController -> SISafetyConfigService : 2a-2. configure(request, userId)
activate SISafetyConfigService
SISafetyConfigService --> CSafetyConfigController : 2a-3. safetyConfigResponse
deactivate SISafetyConfigService
CSafetyConfigController --> JWT : 2a-4. safetyConfigResponse
deactivate CSafetyConfigController
JWT --> UIEnableFallDetectionScreen : 2a-5. 200 OK — safetyConfigResponse
deactivate JWT
UIEnableFallDetectionScreen --> AMother : 2a-6. displayFallDetectionConfiguration()
else [authentication or role authorization fails]
UIEnableFallDetectionScreen -> JWT : 2b. PUT /api/v1/safety/config with invalid or insufficient bearer token
activate JWT
JWT --> UIEnableFallDetectionScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIEnableFallDetectionScreen --> AMother : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIEnableFallDetectionScreen
end
@enduml
```

**Brief Explanation:**

1. The actor starts each grouped use case through the code-reachable UI boundary.
2. Protected requests pass through JwtAuthenticationFilter; rejected credentials or roles return 401 Unauthorized or 403 Forbidden without invoking the controller.
3. The controller receives the request and invokes the exact delegated operation resolved from the current source.
4. The service applies the business policy and coordinates downstream collaborators while its caller remains active.
5. The HTTP response unwinds through middleware when present, and the UI renders the server-authoritative outcome to the actor.

## 5. State Chart Diagram

The lifecycle below belongs to **SafetyMonitoringConfig + ImuMonitoringSession (with the SENSOR_SELF_TEST SafetyEvent nested inside an active session)**. Its states are the persisted constants declared in the sources cited under this diagram, and every transition, guard, and action is taken from the service method that writes that constant. No unreachable state is introduced.

```plantuml
@startuml StateChartDiagram_01FallDetectionConsentConfigurationandSelfTest
hide empty description
[*] --> Unconfigured

Unconfigured --> DetectionDisabled : configure()\n[fallDetectionEnabled == false]\n/ saveSafetyMonitoringConfig()
Unconfigured --> DetectionEnabled : configure()\n[fallDetectionEnabled == true]\n/ saveSafetyMonitoringConfig()
DetectionDisabled --> DetectionEnabled : configure()\n[fallDetectionEnabled == true]\n/ publishSafetyConfigChanged()
DetectionEnabled --> DetectionDisabled : configure()\n[fallDetectionEnabled == false]\n/ publishSafetyConfigChanged()

DetectionEnabled --> Monitoring : enable()\n[sensorConsentGranted && sensorPermissionGranted]\n/ openImuMonitoringSession()
Monitoring --> DetectionEnabled : disable()\n/ stopImuMonitoringSession()

state Monitoring {
  [*] --> SelfTestIdle
  SelfTestIdle --> SelfTestOpen : createSensorSelfTest()\n[signalKey not duplicated]\n/ armCountdownDeadline()
  SelfTestOpen --> SelfTestOpen : createSensorSelfTest()\n[signalKey duplicated]\n/ returnExistingSelfTestEvent()
  SelfTestOpen --> SelfTestTimedOut : completeSensorSelfTest()\n[status == TEST_OPEN]\n/ recordSelfTestResponse()
  SelfTestTimedOut --> SelfTestOpen : createSensorSelfTest()\n/ armCountdownDeadline()
}

DetectionDisabled : fallDetectionEnabled = false
DetectionEnabled : fallDetectionEnabled = true
Monitoring : ImuSessionStatus = ACTIVE
SelfTestOpen : SafetyEventStatus = TEST_OPEN
SelfTestTimedOut : SafetyEventStatus = TIMED_OUT
@enduml
```

**Figure 2 — State Chart Diagram: Fall Detection Consent, Configuration, and Self-Test**

**Brief Explanation:**

1. The user starts in `Unconfigured` because `SafetyConfigStore.findByUserId()` holds no `SafetyMonitoringConfig` row yet.
2. The event `configure()` persists the configuration, and the guard on `fallDetectionEnabled` decides whether the user settles in `DetectionDisabled` or `DetectionEnabled`; the action publishes `SafetyConfigChanged`.
3. The event `enable()` moves the user to `Monitoring` only when the guard `sensorConsentGranted && sensorPermissionGranted` holds, because `FallDetectionService.enable()` calls `requireSensorCollection()` and rejects a missing device permission with `SAFETY-009`; the action opens an `ImuMonitoringSession` with `ImuSessionStatus.ACTIVE`.
4. Inside `Monitoring`, `createSensorSelfTest()` opens a `SENSOR_SELF_TEST` event in `TEST_OPEN` and arms the countdown deadline; a duplicated `signalKey` re-enters `SelfTestOpen` and returns the existing event rather than creating a second one.
5. The event `completeSensorSelfTest()` is accepted only under the guard `status == TEST_OPEN`, and the action records the response before the event settles in `TIMED_OUT` — a self-test never reaches the real alert states.
6. The event `disable()` leaves `Monitoring` from the active session only, stopping it with `ImuSessionStatus.STOPPED` and returning the user to `DetectionEnabled` with the configuration intact.

**State sources:**

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/ImuSessionStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SafetyEventStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/SafetyConfigService.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionService.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/SensorSelfTestService.java`

## 6. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-ES-04` | Consent, device permission, and latest server configuration are required before monitoring. A failed self-test must not be presented as an enabled healthy detector. | No additional gap recorded in the code-first baseline. |

## 7. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 8. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java`
- `05_Development/CareBridgeMobileApp/lib/features/safety/screens/enable_fall_detection_screen.dart`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SensorSelfTestServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/safety/safety_contract_test.dart`
