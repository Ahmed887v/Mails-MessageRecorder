#!/usr/bin/env bash
# يحزم المشروع بالكامل في ملف ZIP واحد قابل للبناء دون إنترنت ودون تثبيت أي أداة.
# يضمّ: الكود المصدري + أدوات البناء المحلية (android-sdk / gradle-8.7 / .gradle-home)
#        + ملفاتك المساعدة (env.sh / build.sh / local.properties).
# الاستخدام:  ./scripts/pack_release.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARENT_DIR="$(dirname "$PROJECT_DIR")"
PROJECT_NAME="$(basename "$PROJECT_DIR")"

# قراءة رقم الإصدار من build.gradle.kts لاستخدامه في اسم الملف
VERSION="$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$PROJECT_DIR/app/build.gradle.kts" 2>/dev/null || echo "dev")"
OUT="$PARENT_DIR/${PROJECT_NAME}-full-${VERSION}.zip"

echo ">> المجلد: $PROJECT_DIR"
echo ">> الإصدار: $VERSION"
echo ">> ملف الخروج: $OUT"

command -v zip >/dev/null 2>&1 || { echo "خطأ: أداة zip غير مثبّتة. ثبّتها: sudo apt install -y zip"; exit 1; }

cd "$PARENT_DIR"

# استثناء مخرجات البناء والمستودع والملفات المؤقتة فقط، مع الإبقاء على الأدوات المحلية
zip -r -q "$OUT" "$PROJECT_NAME" \
  -x "$PROJECT_NAME/app/build/*" \
  -x "$PROJECT_NAME/build/*" \
  -x "$PROJECT_NAME/.gradle/*" \
  -x "$PROJECT_NAME/.git/*" \
  -x "$PROJECT_NAME/*.zip" \
  -x "*/.DS_Store"

echo ""
echo ">> تمّ بنجاح. حجم الحزمة:"
ls -lh "$OUT"
echo ""
echo "لفكّها والبناء لاحقاً في أي وقت (دون إنترنت):"
echo "  unzip -o \"$OUT\""
echo "  cd \"$PROJECT_NAME\" && source env.sh && ./build.sh clean assembleDebug"
