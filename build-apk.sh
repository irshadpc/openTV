#!/bin/bash
# Helper to build the Android TV APK from the command line on macOS.
# Sets up JDK 17 + Android SDK env, installs required SDK packages, then builds.
set -e

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

cd "$(dirname "$0")"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"

# Point Gradle at the SDK.
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Accept licenses and install required packages (idempotent).
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Build the debug APK (no signing keystore needed).
./gradlew --no-daemon assembleDebug

echo ""
echo "APK at: app/build/outputs/apk/debug/app-debug.apk"
