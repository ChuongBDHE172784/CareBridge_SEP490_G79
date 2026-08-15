# syntax=docker/dockerfile:1
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY app/ app/
COPY data/ data/
RUN groupadd --system triage \
    && useradd --system --gid triage --home-dir /app --shell /usr/sbin/nologin triage \
    && chown -R triage:triage /app
USER triage
EXPOSE 8001
# /health returns 503 until the deterministic red-flag probe passes, and urlopen
# raises on 503, so a failing probe keeps the container unhealthy.
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8001/health', timeout=3).read()" || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
