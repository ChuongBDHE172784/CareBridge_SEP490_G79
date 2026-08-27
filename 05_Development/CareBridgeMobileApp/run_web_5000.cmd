@echo off
cd /d "%~dp0"
flutter run -d web-server --web-port 5000 --web-hostname localhost --dart-define=API_BASE_URL=http://localhost:8080 --dart-define=AI_TRIAGE_TIMEOUT_SECONDS=30
