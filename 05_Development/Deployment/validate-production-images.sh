#!/bin/sh
set -eu

found_image=false
invalid_image=false

while IFS= read -r image; do
    [ -n "$image" ] || continue
    found_image=true
    digest=${image##*@sha256:}
    case "$image" in
        *@sha256:*) ;;
        *)
            printf '%s\n' "Mutable production image rejected: $image" >&2
            invalid_image=true
            continue
            ;;
    esac
    if [ "${#digest}" -ne 64 ]; then
        printf '%s\n' "Invalid SHA-256 digest length: $image" >&2
        invalid_image=true
        continue
    fi
    case "$digest" in
        *[!0-9a-f]*)
            printf '%s\n' "Invalid SHA-256 digest characters: $image" >&2
            invalid_image=true
            ;;
    esac
done

if [ "$found_image" != true ]; then
    printf '%s\n' 'No production images were provided for validation.' >&2
    exit 1
fi

if [ "$invalid_image" = true ]; then
    exit 1
fi

printf '%s\n' 'All rendered production images use immutable SHA-256 digests.'
