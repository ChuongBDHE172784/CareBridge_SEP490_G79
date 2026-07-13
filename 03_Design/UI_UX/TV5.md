
### Sprint 0: TV5 - Chương - AI and IMU Safety Foundation

Task: Create AI triage and IMU safety module skeleton only. Sprint 0 does not implement full business logic.

Reference function specs:

- 3.1.2.5 Extract Structured Intake Data
- 3.3.1.37 Run AI Symptom Intake
- 3.3.1.38 View Risk Triage Result
- 3.3.4.1 Configure Safety Monitoring
- 3.3.4.2 Enable Fall Detection
- 3.3.4.3 Disable Fall Detection
- 3.3.4.4 Detect Suspected Fall or Impact

Implementation focus:

- Create backend packages: ai, triage, imu, safety, emergency
- Create mobile folders for AI triage and IMU safety
- Define AI provider interface
- Define IMU sensor provider interface
- Define safety event model
- Define emergency event model
- Draft safety monitoring state machine
- Create mock AI response
- Create mock IMU sensor data source
- Create placeholder API contracts
- No real fall detection logic yet
- No emergency alert logic yet
- No map/location/exercise/posture ownership

Deliverable:

- AI and IMU modules compile successfully
- API contracts are available for later Sprint implementation
- Mock providers are ready
- No complete business flow is required in Sprint 0

### Sprint 1: TV5 - Chương - AI Triage MVP and Basic IMU Monitoring

Task: Build the first runnable MVP for AI triage and basic IMU monitoring.

Function specs:

- 3.1.2.5 Extract Structured Intake Data
- 3.3.1.37 Run AI Symptom Intake
- 3.3.1.38 View Risk Triage Result
- 3.3.1.39 Open Emergency Flow
- 3.3.4.1 Configure Safety Monitoring
- 3.3.4.2 Enable Fall Detection
- 3.3.4.3 Disable Fall Detection

Implementation focus:

- User enters symptom description
- System extracts structured intake data from symptom input
- AI triage classifies risk level as Green, Yellow, or Red
- Show risk triage result screen
- Open emergency flow when risk level is high
- Enable or disable IMU monitoring
- Read accelerometer data
- Read gyroscope data
- Show IMU monitoring status
- Show basic sensor values for demo
- No automatic fall detection yet
- No safety countdown yet
- No emergency alert sending yet

Deliverable:

- User can run AI symptom intake and view risk result
- User can enable or disable IMU monitoring
- App can read and display basic IMU sensor data

### Sprint 2: TV5 - Chương - IMU Fall Detection and Emergency Safety Logic

Task: Implement the core IMU safety business logic.

Function specs:

- 3.3.1.42 Send Family Emergency Alert
- 3.3.4.4 Detect Suspected Fall or Impact
- 3.3.4.5 Confirm Safety Check
- 3.3.4.6 Send Emergency Alert
- 3.3.4.7 View Safety Event History
- 3.3.4.8 Report False Positive Detection
- 3.3.4.10 Configure Emergency Contact

Implementation focus:

- Detect abnormal acceleration or impact pattern
- Create suspected fall or impact event
- Start safety confirmation countdown
- Allow user to confirm that they are safe
- Trigger emergency alert when user does not confirm safety
- Configure emergency contact
- Save safety event history
- Allow user to report false positive detection
- Improve event status: suspected, safe_confirmed, alert_sent, false_positive

Deliverable:

- System can simulate or detect suspected fall/impact
- Safety countdown works
- User can confirm safety
- Emergency alert can be triggered
- Safety event history is stored
- False positive feedback is recorded

### Sprint 3: TV5 - Chương - AI and IMU Emergency Integration

Task: Integrate AI triage and IMU safety events with notification, family alert, health context, and map handoff.

Function specs:

- 3.3.1.39 Open Emergency Flow
- 3.3.1.42 Send Family Emergency Alert
- 3.3.4.6 Send Emergency Alert
- 3.3.4.9 Open Emergency Support from Safety Alert

Integration focus:

- Connect emergency alert with TV1 notification service
- Connect family emergency alert with TV2 care group or family contact data
- Connect emergency support handoff with TV4 map/location provider
- Attach AI triage result to emergency flow
- Attach IMU safety event data to emergency flow
- Ensure TV5 only sends location/map request to TV4 API
- Do not implement map, route, ETA, nearby facility, or navigation logic inside TV5 module

Deliverable:

- AI high-risk result can open emergency flow
- IMU safety alert can open emergency support
- Emergency alert is sent through notification service
- Family alert uses care group or emergency contact data
- Map/navigation handoff is delegated to TV4


### Sprint 4: TV5 - Chương - AI Provider Upgrade and IMU Optimization

Task: Upgrade mock providers, optimize IMU detection, and improve safety reliability.

Function specs:

- 3.1.2.5 Extract Structured Intake Data
- 3.3.1.37 Run AI Symptom Intake
- 3.3.1.38 View Risk Triage Result
- 3.3.4.4 Detect Suspected Fall or Impact
- 3.3.4.5 Confirm Safety Check
- 3.3.4.6 Send Emergency Alert
- 3.3.4.7 View Safety Event History
- 3.3.4.8 Report False Positive Detection

Implementation focus:

- Replace mock AI provider with real or sandbox provider where available
- Improve AI prompt for symptom intake
- Improve structured data extraction accuracy
- Tune IMU threshold for fall/impact detection
- Add noise filtering for accelerometer and gyroscope data
- Reduce false positives from normal activities
- Test edge cases: phone dropped, walking, running, sudden movement, sitting down quickly
- Improve emergency alert reliability
- Add fallback behavior when AI provider or sensor provider fails

Deliverable:

- AI triage is more reliable and demo-ready
- IMU detection is more stable
- False positives are reduced
- Emergency flow has fallback behavior


### Sprint 5: TV5 - Chương - Final Stabilization and Demo Freeze

Task: Stabilize AI triage and IMU safety monitoring for final demo.

Final checks:

- AI symptom intake works
- Structured intake extraction works
- Risk triage result is displayed correctly
- High-risk AI result can open emergency flow
- IMU monitoring can be enabled and disabled
- Suspected fall or impact can be simulated
- Safety countdown works
- User can confirm safety
- Emergency alert is sent when user does not confirm safety
- Safety event history is recorded
- False positive report works
- Emergency alert integrates with TV1 notification
- Emergency contact or family alert integrates with TV2 data
- Map/navigation handoff goes to TV4
- No map/location code is owned by TV5
- No pregnancy exercise or posture code is owned by TV5

Demo path:
Run AI triage
→ Get high-risk result
→ Open emergency flow
→ Enable IMU safety monitoring
→ Simulate suspected fall or impact
→ Safety countdown starts
→ User does not confirm safety
→ Emergency alert is sent
→ Family receives alert
→ Map/navigation handoff is requested from TV4
