# CompreFace for expert identity verification

This optional stack pins the official CompreFace images to `1.2.0`. CareBridge uses only its Face Verification API to compare the submitted selfie with the portrait visible on the front of the CCCD. A similarity result is supporting evidence only: it does not prove that a CCCD is authentic and never approves an expert automatically.

## Docker Desktop (Windows / macOS) -- recommended path

Compose runs containers inside a Linux VM managed by Docker Desktop, so x86/AVX is provided by the host CPU regardless of the client OS. Allocate at least **5 GB** to Docker Desktop (Settings -> Resources -> Memory) before starting this profile.

## Setup checklist

1. Allocate **5 GB RAM** in Docker Desktop settings. (OOM kills are the most common failure mode with smaller allocations.)
2. Copy `.env.example` to `.env` in the Backend folder, set a strong `COMPREFACE_POSTGRES_PASSWORD`, and keep `COMPREFACE_ENABLED=false` initially.
3. Start explicitly with:
   ```
   docker compose -f docker-compose.staging.yml -f docker-compose.compreFace.yml --profile compreface up -d
   ```
4. Wait ~60 s for the ONNX model to load, open `http://localhost:8000/login`, create a **Face Verification** service, and copy its API key to `COMPREFACE_API_KEY` in `.env`.
5. Restart the Backend so it picks up the new env. Set `COMPREFACE_ENABLED=true` only after connectivity is confirmed.
6. Run one or two controlled similarity samples in the Admin UI, then set `COMPREFACE_SIMILARITY_THRESHOLD` based on observed real-match vs impostor distributions. Keep manual admin review enabled for every result and for timeouts / multiple-face errors.

## Memory budget (5 GB Docker allocation)

| Container | Heap / reserved | Why |
|-----------|-----------------|-----|
| core (Python ONNX) | ~800 MB resident | face-recognition lib loads ONNX models |
| api (Spring) | -Xmx1500m | capped below Docker default 4g |
| admin (Spring) | -Xmx1g | lighter, UI only |
| front-end (nginx) | ~40 MB | static files |
| Postgres | ~100 MB | small metadata DB |
| Python / uWSGI | ~60 MB | process overhead |

Watch `docker stats` while loading the first face -- the ONNX model can spike briefly. If containers get OOM-killed, reduce `API_JAVA_OPTS` (api) or `UWSGI_PROCESSES` (core) and retry.

## Important security notes

- CompreFace credentials are backend-only and must never be exposed to Web/Mobile or committed files.
- Face Verification results are supporting evidence only; an expert is never approved automatically.
- The compose profile is deliberately not part of the default staging startup.
