# syntax=docker/dockerfile:1
FROM python:3.10.16-slim-bookworm@sha256:f9fd9a142c9e3bc54d906053b756eb7e7e386ee1cf784d82c251cf640c502512

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    EXERCISE_MODEL_DIR=/app/models \
    EXERCISE_MODEL_MANIFEST=/app/models/SHA256SUMS

RUN apt-get update \
    && apt-get install --yes --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 exercise \
    && useradd --system --uid 10001 --gid exercise --home-dir /app --shell /usr/sbin/nologin exercise

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir --only-binary=:all: -r requirements.txt

COPY --chown=exercise:exercise app ./app
COPY --chown=exercise:exercise scripts ./scripts
COPY --chown=exercise:exercise models ./models

USER 10001:10001
EXPOSE 8002
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8002/health', timeout=2).read()"]
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8002", "--workers", "1", "--no-access-log"]
