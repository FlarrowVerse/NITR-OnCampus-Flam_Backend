#!/usr/bin/env bash
set -e

echo "🧹 Cleaning old builds..."
./gradlew clean

echo "⚙️ Building project..."
./gradlew build

echo "📦 Installing distribution..."
./gradlew installDist

echo "✅ Done! Run your app from: build/install/queuectl/bin/queuectl"
