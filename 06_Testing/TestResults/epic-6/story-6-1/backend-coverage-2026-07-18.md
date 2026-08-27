# Story 6.1 Backend Changed-Code Coverage

- Measured: 2026-07-18 (Asia/Saigon)
- Tool: JaCoCo Maven Plugin 0.8.13
- Result: **PASS** — changed service/policy line coverage is **90.83%** (297/327), above the 80% exit threshold.
- Reported branch coverage: **60.53%** (92/152). The Story 6.1 Test-Spec requires branch coverage to be reported, but does not set a branch threshold.

## Test execution

```text
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The measured test set was:

- `JourneyCanonicalLifecycleServiceTest`
- `JourneyCanonicalLifecycleControllerTest`
- `JourneyCanonicalLifecycleIntegrationTest`
- `JourneyServiceImplTest`
- `JourneyDashboardServiceImplTest`
- `AuditEligibilityPolicyTest`

Command:

```powershell
.\mvnw.cmd "-Dtest=JourneyCanonicalLifecycleServiceTest,JourneyCanonicalLifecycleControllerTest,JourneyCanonicalLifecycleIntegrationTest,JourneyServiceImplTest,JourneyDashboardServiceImplTest,AuditEligibilityPolicyTest" "-Djacoco.destFile=target/jacoco-story61.exec" org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test "-Djacoco.dataFile=target/jacoco-story61.exec" org.jacoco:jacoco-maven-plugin:0.8.13:report
```

## Changed-code calculation

| Source | Coverage scope | Lines | Line coverage | Branches | Branch coverage |
| --- | --- | ---: | ---: | ---: | ---: |
| `JourneyTransitionServiceImpl.java` | All executable lines; new Story 6.1 file | 263/280 | 93.93% | 71/106 | 66.98% |
| `JourneyTransitionPolicy.java` | All executable lines; new Story 6.1 file | 19/25 | 76.00% | 18/38 | 47.37% |
| `JourneyServiceImpl.java` | Executable lines intersecting added/modified diff hunks | 15/22 | 68.18% | 3/8 | 37.50% |
| **Aggregate** | **Story 6.1 service/policy changed code** | **297/327** | **90.83%** | **92/152** | **60.53%** |

JaCoCo XML source-line counters were intersected with the zero-context Git diff for the existing `JourneyServiceImpl.java`. All executable source lines were counted for the two new service/policy files. DTOs, entities, repositories, interfaces, controllers, generated code, and unchanged legacy lines are outside the service/policy changed-code threshold defined by the Test-Spec.

The generated machine-readable report remains at `05_Development/CareBridgeAPI/target/site/jacoco/jacoco.xml` in the local build workspace.
