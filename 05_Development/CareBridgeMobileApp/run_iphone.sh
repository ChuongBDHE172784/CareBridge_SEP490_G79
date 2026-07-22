#!/bin/bash
# Run the app in release mode on the physical iPhone, with API_BASE_URL pointed
# at this Mac's LAN IP so the device (not just the simulator) can reach the
# backend. Plain `flutter run --release` defaults to localhost:8080, which on
# a real device means the phone itself, not this machine — always fails.
set -euo pipefail

cd "$(dirname "$0")"

DEVICE_NAME="${1:-Promax}"
LAN_IP=$(ipconfig getifaddr en0)

if [ -z "$LAN_IP" ]; then
  echo "Could not determine LAN IP via en0. Is Wi-Fi connected?" >&2
  exit 1
fi

DEVICE_ID=$(flutter devices --machine 2>/dev/null | python3 -c "
import json, sys
devices = json.load(sys.stdin)
matches = [d for d in devices if '$DEVICE_NAME'.lower() in d.get('name', '').lower() and d.get('targetPlatform', '').startswith('ios')]
print(matches[0]['id'] if matches else '')
")

if [ -z "$DEVICE_ID" ]; then
  echo "No connected iOS device matching '$DEVICE_NAME' found. Run 'flutter devices' to check." >&2
  exit 1
fi

echo "Backend LAN IP: $LAN_IP"
echo "Target device:  $DEVICE_ID ($DEVICE_NAME)"
echo "Make sure the backend is running and the phone is unlocked, then continuing..."

flutter run --release -d "$DEVICE_ID" --dart-define=API_BASE_URL="http://$LAN_IP:8080"
