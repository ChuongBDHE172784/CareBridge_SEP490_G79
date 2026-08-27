# MF-01 / Story 6.1 — Mobile manual test evidence

## Execution

| Field | Value |
| --- | --- |
| Date | 2026-07-18 |
| Device | Samsung SM-N986N, Android 13 |
| App | `com.carebridge.app` version `1.0.0` (`versionCode=1`) |
| Repository commit | `e07bc25f` |
| Screen | `1080x2316` override, `420 dpi` |
| Overall | 2 failed, 2 partially executed, 12 blocked |

The runtime account and API environment could not be mapped to approved synthetic fixtures. No lifecycle create/update action was submitted.

## Evidence map

| Evidence | Observation |
| --- | --- |
| `01-initial-screen.png` | Existing session opens Home; generic Mother state |
| `02-journey-screen.png` | Child tab has no child profile |
| `03-pregnancy-tab.png` | Pregnancy tab reports no pregnancy journey |
| `04-setup-step1.png` | Four calculation methods; continue disabled before selection |
| `05-setup-step2.png` | Gestational-age input at 4 weeks 0 days |
| `06-wizard-resume.png` | Step 2 preserved after 30-second background/resume |
| `07-setup-result.png` | Result: 4 weeks 0 days, EDD 27 March 2027 |
| `08-result-resume.png` | Result preserved after 30-second background/resume |
| `09-result-font150.png` | At 150% font, the `Kết quả` heading is clipped/obscured |
| `10-result-landscape.png` | Landscape rendering evidence |
| `11-profile.png` | Profile top; no journey-history entry |
| `12-profile-bottom.png` | Profile middle; no journey-history entry |
| `13-profile-end.png` | Profile end; no journey-history entry |

Each PNG has a corresponding UI hierarchy XML where captured. The XML for wizard back buttons shows `NAF=true` and an empty `content-desc`.

## Safety/restoration

- Font scale restored to `1.0`.
- Auto-rotation restored to `accelerometer_rotation=1`, `user_rotation=0`.
- App was left on the Journey tab.
- No account logout, token manipulation, network shutdown, lifecycle create, lifecycle update, or database mutation was performed.
