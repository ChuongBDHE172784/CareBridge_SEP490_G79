# syntax=docker/dockerfile:1.7
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    GEMINI_ENABLED=false \
    CAREBRIDGE_AI_SERVICE_PATH=/opt/carebridge/ai-service

WORKDIR /opt/carebridge/evaluator

COPY . /opt/carebridge/evaluator
COPY --from=ai-service . /opt/carebridge/ai-service

RUN python -m pip install --no-cache-dir /opt/carebridge/evaluator \
    && python -m pip install --no-cache-dir -r /opt/carebridge/ai-service/requirements.txt \
    && groupadd --system evaluator \
    && useradd --system --gid evaluator --home-dir /home/evaluator --create-home evaluator \
    && mkdir -p /reports \
    && chown -R evaluator:evaluator /reports /opt/carebridge

USER evaluator

ENTRYPOINT ["carebridge-eval"]
CMD ["run", "--mode", "local", "--output-dir", "/reports/latest"]

