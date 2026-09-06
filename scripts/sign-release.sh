#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
DESPERTA_SIGNING_DIR="${DESPERTA_SIGNING_DIR:-$HOME/.config/desperta-signing}"
DESPERTA_BUILD_TOOLS="${DESPERTA_BUILD_TOOLS:-36.0.0}"
./gradlew :app:assembleRelease
mkdir -p dist
"$ANDROID_HOME/build-tools/$DESPERTA_BUILD_TOOLS/zipalign" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/Desperta-aligned.apk
"$ANDROID_HOME/build-tools/$DESPERTA_BUILD_TOOLS/apksigner" sign --ks "$DESPERTA_SIGNING_DIR/release.jks" --ks-key-alias desperta --ks-pass "file:$DESPERTA_SIGNING_DIR/password" --out dist/Desperta-1.0.0.apk dist/Desperta-aligned.apk
"$ANDROID_HOME/build-tools/$DESPERTA_BUILD_TOOLS/apksigner" verify --verbose dist/Desperta-1.0.0.apk
(cd dist && shasum -a 256 Desperta-1.0.0.apk > SHA256SUMS.txt)
