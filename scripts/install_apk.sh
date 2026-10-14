#!/usr/bin/env bash
# سكربت تثبيت APK على جهاز أندرويد موصول عبر adb
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

if ! command -v adb >/dev/null 2>&1 && [ ! -x "$ADB" ]; then
  echo "خطأ: adb غير موجود. ثبّت platform-tools عبر sdkmanager."
  exit 1
fi

ADB_BIN="$(command -v adb || echo "$ADB")"

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$DEBUG_APK" ]; then
  echo "خطأ: لم يتم العثور على $DEBUG_APK. شغّل scripts/build_apk.sh أولاً."
  exit 1
fi

echo ">> الأجهزة المتصلة:"
"$ADB_BIN" devices

echo ">> تثبيت $DEBUG_APK ..."
"$ADB_BIN" install -r "$DEBUG_APK"

echo "تم التثبيت. افتح التطبيق ثم فعّل الصلاحيات من الإعدادات."
