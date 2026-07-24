# syntax=docker/dockerfile:1.7
FROM python:3.12-alpine3.23

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    GEMINI_ENABLED=false \
    CAREBRIDGE_AI_SERVICE_PATH=/opt/carebridge/ai-service

WORKDIR /opt/carebridge/evaluator

COPY . /opt/carebridge/evaluator
COPY --from=ai-service . /opt/carebridge/ai-service

RUN python -m pip install --no-cache-dir -e /opt/carebridge/evaluator \
    && python -m pip install --no-cache-dir -r /opt/carebridge/ai-service/requirements.txt \
    && addgroup -S evaluator \
    && adduser -S -G evaluator -h /home/evaluator evaluator \
    && mkdir -p /reports \
    && chown -R evaluator:evaluator /reports /opt/carebridge

USER evaluator

ENTRYPOINT ["carebridge-eval"]
CMD ["run", "--mode", "local", "--output-dir", "/reports/latest"]

