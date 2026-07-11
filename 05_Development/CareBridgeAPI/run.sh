#!/usr/bin/env bash
# run.sh — load .env, export, then run Spring Boot with correct profile
set -e
cd "$(dirname "$0")"

# Load .env and export _only_ KEY=VALUE lines (skip JSON blocks and comments)
while IFS= read -r line || [[ -n "$line" ]]; do
    # Trim leading/trailing whitespace
    trimmed="${line#"${line%%[![:space:]]*}"}"
    trimmed="${trimmed%"${trimmed##*[![:space:]]}"}"
    # Skip blanks, comments, and JSON objects
    [[ -z "$trimmed" ]] && continue
    [[ "$trimmed" == \#* ]] && continue
    [[ "$trimmed" == \{* ]] && continue
    # Split on first = only
    key="${trimmed%%=*}"
    val="${trimmed#*=}"
    # Strip surrounding quotes from value
    val="${val#\"}"
    val="${val%\"}"
    export "$key=$val"
done < .env

exec ./mvnw spring-boot:run
