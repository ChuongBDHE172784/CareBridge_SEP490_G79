# Exercise-Correction sidecar

Private, landmark-only inference service for the CareBridge concept demo. It vendors the four
general-exercise models reviewed at upstream commit
`202a0a802d8d2e3ea42f00e6a8c47da9cafc09d7`. These models are not validated for pregnancy,
clinical decisions, or the CareBridge population.

The process verifies every artifact against `models/SHA256SUMS`, loads the eight pickle/scaler
files once, and becomes ready only after all four model bundles load. Requests cannot select a
file or model path. The service accepts named MediaPipe landmarks only; it does not accept or
store frames, JWTs, CareBridge identities, or database credentials.

Run through the private Compose overlay from the repository root:

```sh
docker compose -f docker-compose.yml -f 05_Development/Deployment/docker-compose.exercise-ml.yml up --build
docker compose -f docker-compose.yml -f 05_Development/Deployment/docker-compose.exercise-ml.yml \
  exec exercise-correction python scripts/smoke.py
```

The overlay deliberately publishes no sidecar host port. Spring reaches it through the internal
service name `http://exercise-correction:8002`.
