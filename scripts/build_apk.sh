#!/usr/bin/env bash
# سكربت بناء APK لتطبيق NewRecorder على Kali Linux
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME

# التحقق من Java 17
if ! command -v java >/dev/null 2>&1; then
  echo "خطأ: Java غير مثبّت. ثبّته عبر: sudo apt install -y openjdk-17-jdk"
  exit 1
fi

# التحقق من Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
  echo "خطأ: لم يتم العثور على Android SDK في: $ANDROID_HOME"
  echo "ثبّت cmdline-tools وحدّد ANDROID_HOME كما في README."
  exit 1
fi

# إنشاء local.properties تلقائياً
echo "sdk.dir=$ANDROID_HOME" > local.properties

# تجهيز Gradle Wrapper 8.7 إن لم يوجد
if [ ! -x "./gradlew" ]; then
  if command -v gradle >/dev/null 2>&1; then
    echo ">> إنشاء Gradle Wrapper 8.7 ..."
    gradle wrapper --gradle-version 8.7
  else
    echo "خطأ: لا يوجد gradle. ثبّته عبر: sudo apt install -y gradle"
    exit 1
  fi
fi

echo ">> بناء نسخة التطوير (Debug)..."
./gradlew assembleDebug

echo ">> بناء نسخة الإصدار (Release غير موقّعة)..."
./gradlew assembleRelease

echo ""
echo "تم البناء بنجاح:"
ls -1 app/build/outputs/apk/debug/*.apk 2>/dev/null || true
ls -1 app/build/outputs/apk/release/*.apk 2>/dev/null || true
