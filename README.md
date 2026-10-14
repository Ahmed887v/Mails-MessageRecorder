# NewRecorder

تطبيق أندرويد أصلي لإدارة صندوق وارد موحّد: يجمع الرسائل والتنبيهات غير المقروءة من
Gmail وWhatsApp وMessenger وInstagram وSnapchat وSMS في شجرة واحدة (التطبيق → الحساب → الرسائل).

مبني بـ Kotlin + Jetpack Compose وفق نمط MVVM، ويعمل محلياً بالكامل.

---

## الميزات

- صندوق وارد موحّد بتبويبات: Gmail / WhatsApp / SMS / أخرى.
- التقاط لحظي للإشعارات عبر `NotificationListenerService` (يقرأ المحادثات والمرسل والنص).
- التقاط فوري لرسائل SMS + أرشفة الرسائل القديمة.
- ربط Gmail عبر OAuth 2.0 (بدون كلمات مرور) وجلب البريد عبر IMAP/XOAUTH2.
- تمييز الحسابات الأساسية عن المزدوجة (Cloned Apps) عبر معرّف الملف الشخصي.
- حلّ مشكلة تأخّر الإشعارات: خدمة أمامية دائمة + تجاهل تحسين البطارية + مزامنة دورية.
- تخزين محلي بالكامل عبر Room، ورموز OAuth مشفّرة.

---

## المتطلبات

- نظام تشغيل البناء: Kali Linux (أو أي Debian/Ubuntu).
- اتصال إنترنت (لتحميل الاعتماديات).
- جهاز أندرويد (API 26+) للاختبار، أو محاكي.

يمكن بناء التطبيق على Kali Linux عبر الخطوات التالية من التيرمينال بالكامل.

---

## 1) تجهيز البيئة على Kali Linux

### تحديث الحزم وتثبيت Java 17

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk unzip wget curl zip
```

### التحقق من Java

```bash
java -version
```

يجب أن يظهر إصدار 17.

### تثبيت Android SDK Command-line Tools

```bash
mkdir -p "$HOME/android-sdk/cmdline-tools"
cd "$HOME/android-sdk/cmdline-tools"
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
```

### ضبط متغيرات البيئة

```bash
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT=$HOME/android-sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$HOME/android-sdk/cmdline-tools/latest/bin:$HOME/android-sdk/platform-tools' >> ~/.zshrc
source ~/.zshrc
```

### قبول التراخيص وتثبيت المكوّنات المطلوبة

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### تثبيت Gradle (لاستخدامه لإنشاء الـ wrapper فقط)

```bash
sudo apt install -y gradle
gradle -v
```

> مهم: سيبني المشروع عبر Gradle Wrapper 8.7 المتوافق مع Android Gradle Plugin 8.5.2.
> يُنشأ الـ wrapper مرة واحدة بالأمر التالي من داخل مجلد المشروع:
> `gradle wrapper --gradle-version 8.7`

---

## 2) تنزيل المشروع وفتحه

```bash
unzip NewRecorder.zip -d NewRecorder
cd NewRecorder
```

### إنشاء ملف local.properties

يشير إلى مسار Android SDK على جهازك.

```bash
echo "sdk.dir=$HOME/android-sdk" > local.properties
```

---

## 3) بناء التطبيق (APK)

### إنشاء Gradle Wrapper (مرة واحدة)

```bash
gradle wrapper --gradle-version 8.7
```

### بناء نسخة التطوير (Debug)

```bash
./gradlew assembleDebug
```

ينتج الملف في:
`app/build/outputs/apk/debug/app-debug.apk`

### بناء نسخة الإصدار (Release غير موقّعة)

```bash
./gradlew assembleRelease
```

### (اختياري) توقيع نسخة الإصدار

أنشئ مفتاحاً للتوقيع:

```bash
keytool -genkeypair -v -keystore newrecorder.jks -alias newrecorder \
  -keyalg RSA -keysize 2048 -validity 10000
```

وقّع الـ APK (يتطلب build-tools):

```bash
zipalign -v -p 4 app/build/outputs/apk/release/app-release-unsigned.apk app-aligned.apk
apksigner sign --ks newrecorder.jks --out app-release-signed.apk app-aligned.apk
```

> مسار الأدوات:
> `$ANDROID_HOME/build-tools/34.0.0/zipalign` و `$ANDROID_HOME/build-tools/34.0.0/apksigner`

---

## 4) تثبيت التطبيق على الجهاز

### تفعيل وضع المطوّر

على الهاتف: الإعدادات → حول الهاتف → اضغط "رقم الإصدار" 7 مرات → خيارات المطوّر → فعّل "تصحيح USB".

### التحقق من الاتصال

```bash
adb devices
```

### تثبيت ملف الـ APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### بديل: التثبيت اللاسلكي (Android 11+)

```bash
adb pair 192.168.1.50:37000
adb connect 192.168.1.50:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5) تفعيل صلاحيات التطبيق بعد التثبيت (مهم)

افتح التطبيق ثم الإعدادات واتبع الآتي:

1. **الوصول إلى الإشعارات**: اضغط "تفعيل" ثم اسمح لـ NewRecorder. (ضروري لقراءة تنبيهات الشات)
2. **إشعارات التطبيق**: اسمح بها (Android 13+).
3. **تجاهل تحسين البطارية**: فعّله لمنع تنويم التطبيق.
4. **صلاحية SMS**: للحصول على التقاط الرسائل النصية وأرشفتها.

---

## 6) ربط Gmail (OAuth 2.0) — خطوات لمرة واحدة

لجلب البريد مباشرة تحتاج إنشاء معرّف عميل OAuth من Google Cloud Console:

1. افتح https://console.cloud.google.com واختر مشروعاً.
2. فعّل "Gmail API".
3. أنشئ OAuth Client ID من نوع **Android**.
4. أضف اسم الحزمة: `com.newrecorder.app`.
5. أضف بصمة التوقيع (SHA-1).

### استخراج بصمة SHA-1 الخاصة بمفتاح التصحيح

```bash
keytool -list -v -keystore "$HOME/.android/debug.keystore" \
  -alias androiddebugkey -storepass android -keypass android
```

### وضع المعرّف في الكود

افتح الملف:
`app/src/main/java/com/newrecorder/app/gmail/GmailOAuthManager.kt`

واستبدل قيمة `OAUTH_CLIENT_ID` بمعرّف العميل الذي حصلت عليه.

ثم أعد البناء والتثبيت. بعدها يظهر زر "تسجيل الدخول باستخدام جوجل" في الإعدادات.

---

## 7) سكربتات مساعدة

- `scripts/build_apk.sh`: يضبط البيئة ويبني APK تلقائياً.
- `scripts/install_apk.sh`: يثبّت آخر APK مبني عبر adb.

### تشغيل البناء عبر السكربت

```bash
bash scripts/build_apk.sh
```

### التثبيت عبر السكربت

```bash
bash scripts/install_apk.sh
```

---

## هيكل المشروع

```
NewRecorder/
├── PRD.md                 متطلبات المنتج
├── README.md              هذا الملف
├── docs/
│   └── ARCHITECTURE.md    شرح البنية المعمارية
├── branding/
│   ├── icon-512.png       أيقونة العلامة
│   ├── icon-192.png
│   └── gen_icon.py        مولّد الأيقونة
├── scripts/
│   ├── build_apk.sh
│   └── install_apk.sh
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/newrecorder/app/...
        └── res/...
```

---

## حلّ المشكلات الشائعة

- **`SDK location not found`**: تأكد من وجود `local.properties` ومحتواه `sdk.dir`.
- **فشل قبول التراخيص**: أعد تشغيل `yes | sdkmanager --licenses`.
- **لا تُلتقط الإشعارات**: تأكد من تفعيل "الوصول إلى الإشعارات" للتطبيق.
- **توقّف الجلب في الخلفية**: فعّل "تجاهل تحسين البطارية" و"الخدمة المستمرة".
- **بطء أول بناء**: طبيعي، إذ تُحمّل اعتماديات Gradle أول مرة.

### البناء يعلق في البداية (Stuck) لساعات

إذا توقّف التقدّم عند نسبة منخفضة (مثلاً 9%) لفترة طويلة، فالسبب غالباً تعليق تنزيل
الاعتماديات من المستودعات الرسمية. اتبع الآتي:

أوقف البناء الحالي ثم أوقف الـ daemon:

```bash
./gradlew --stop
```

تحقق من استهلاك الشبكة واتصال المستودعات (من نافذة تيرمينال أخرى):

```bash
curl -I --max-time 10 https://maven.aliyun.com/repository/google/
curl -I --max-time 10 https://dl.google.com/
```

راقب نمو مجلد ذاكرة التخزين للتأكد هل التنزيل يتقدّم فعلاً:

```bash
du -sh ~/.gradle/caches
```

شغّل البناء مع سجل تفصيلي لمعرفة المهمة العالقة بالضبط:

```bash
./gradlew assembleDebug --info --console=plain
```

يحتوي هذا المشروع على مرايا علي بابا السريعة مضبوطة مسبقاً في `settings.gradle.kts`.
إن كانت شبكتك بطيئة، استخدم أي وكيل (proxy) عبر ملف `gradle.properties` في منزل المستخدم:

```bash
cat > ~/.gradle/gradle.properties << 'EOF'
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=8080
EOF
```

إن كان هناك اعتماد ناقص أو تالف من محاولة سابقة، فالحل الآمن هو نقل مجلد الاعتماديات
المؤقت جانباً (بدلاً من حذفه) ثم إعادة البناء:

```bash
mv ~/.gradle/caches/modules-2 ~/.gradle/caches/modules-2.bak
./gradlew assembleDebug
```

نصيحة: أول بناء ناجح يخزّن كل شيء محلياً، والبناءات التالية تكون سريعة. يمكن أيضاً
تشغيل البناء دون اتصال بعد نجاحه أول مرة:

```bash
./gradlew assembleDebug --offline
```


---

## الأمان والخصوصية

- لا خوادم خارجية؛ كل البيانات على الجهاز.
- رموز OAuth مشفّرة (EncryptedSharedPreferences) ومستثناة من النسخ الاحتياطي.
- لا تُخزَّن كلمات مرور حسابات Google إطلاقاً.
