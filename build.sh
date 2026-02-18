#!/bin/bash
# Build script for FastER Festival App
# This script builds the APK without relying on gradle wrapper download

set -e

PROJECT_DIR="/Users/umasenthil/FastER"
SDK_ROOT="/Users/umasenthil/Library/Android/sdk"

echo "FastER Festival - Build Script"
echo "=============================="
echo ""
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found! Please install JDK 11+"
    exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# Check SDK
if [ ! -d "$SDK_ROOT" ]; then
    echo "❌ Android SDK not found at $SDK_ROOT"
    exit 1
fi
echo "✅ Android SDK: $SDK_ROOT"

echo ""
echo "Building FastER Festival App..."
echo ""

# Navigate to project
cd "$PROJECT_DIR"

# Try using gradle from SDK
GRADLE_BIN="$SDK_ROOT/tools/bin/gradle"

if [ -x "$GRADLE_BIN" ]; then
    echo "Using Android SDK gradle..."
    "$GRADLE_BIN" clean build -x test
else
    echo "Using local gradlew..."
    ./gradlew clean build -x test
fi

echo ""
echo "✅ Build completed successfully!"
echo ""
echo "Generated APK: app/build/outputs/apk/debug/app-debug.apk"
