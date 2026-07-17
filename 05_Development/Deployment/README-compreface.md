# CompreFace for expert identity verification

This optional stack pins the official CompreFace images to `1.2.0`. CareBridge uses only its Face Verification API to compare the submitted selfie with the portrait visible on the front of the CCCD. A similarity result is supporting evidence only: it does not prove that a CCCD is authentic and never approves an expert automatically.

## Linux verification checklist

Live verification is **PENDING_LINUX_VERIFICATION**. Do not start this stack on the current Windows development environment.

1. Confirm x86 AVX support with `lscpu | grep avx` and provide enough memory for the API/core containers.
2. Copy `.env.example` to the server-only `.env`, set a strong CompreFace database password, and keep `COMPREFACE_ENABLED=false` initially.
3. Start explicitly with `docker compose -f docker-compose.staging.yml -f docker-compose.compreFace.yml --profile compreface up -d`.
4. Wait for model loading, open `http://<host>:8000/login`, create a **Face Verification** service, and copy its API key only to the Backend environment.
5. From the shared Docker network, configure `COMPREFACE_BASE_URL=http://compreface-fe`, validate timeouts/health, then run controlled similarity samples before choosing a production threshold.
6. Set `COMPREFACE_ENABLED=true` only after the Backend can reach the service. Keep manual admin review enabled for every result and for timeouts/multiple-face errors.

The compose profile is deliberately not part of the default staging startup. R2 and CompreFace credentials must never be placed in Web/Mobile configuration or committed files.
