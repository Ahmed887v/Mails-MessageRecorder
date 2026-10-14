# نظرة عامة

مشروع أندرويد أصلي مبني بـ **Kotlin + Jetpack Compose** وفق نمط **MVVM**، مع **Room** للتخزين المحلي، و**Coroutines/Flow** لمعالجة البيانات في الخلفية.

## البنية المعمارية

```
app/src/main/java/com/newrecorder/app/
├── NewRecorderApp.kt            تهيئة التطبيق وحاوية الاعتماديات + قنوات الإشعارات
├── MainActivity.kt              نقطة الدخول (Compose)
├── di/
│   └── AppContainer.kt          حقن اعتماديات يدوي (Manual DI)
├── data/
│   ├── local/
│   │   ├── NewRecorderDatabase.kt   قاعدة Room (SQLite)
│   │   ├── Converters.kt            محوّل الأنواع (enums)
│   │   ├── entity/                  AppEntity, AccountEntity, MessageEntity + العلاقات
│   │   └── dao/                     AppDao, AccountDao, MessageDao
│   ├── preferences/
│   │   ├── SettingsStore.kt         إعدادات DataStore
│   │   └── EncryptedTokenStore.kt   تخزين رموز OAuth مشفّر
│   └── repository/
│       └── InboxRepository.kt       المصدر الوحيد للحقيقة (شجرة موحّدة)
├── gmail/
│   ├── GmailOAuthManager.kt     OAuth 2.0 (Authorization Code + PKCE) عبر AppAuth
│   ├── ImapClient.kt            جلب غير المقروء عبر IMAP/XOAUTH2
│   └── GmailRepository.kt       ربط المصادقة بالجلب بالمستودع
├── service/
│   ├── NotificationCollectorService.kt   NotificationListenerService (الشات + الحسابات المزدوجة)
│   ├── SmsReceiver.kt                    التقاط SMS لحظياً
│   ├── SyncService.kt                    خدمة أمامية دائمة (Foreground Service)
│   ├── ImapSyncWorker.kt                 مزامنة دورية (WorkManager)
│   └── BootReceiver.kt                   إعادة التشغيل بعد الإقلاع
├── ui/
│   ├── theme/                            Material 3
│   ├── navigation/                       NavHost + Routes
│   ├── dashboard/                        لوحة الوارد + التبويبات
│   ├── accounts/                         قائمة الحسابات
│   ├── messages/                         الرسائل غير المقروءة
│   ├── settings/                         الإعدادات + صلاحيات + ربط Gmail
│   └── components/                       عناصر مشتركة
└── util/                                 أدوات مساعدة (صلاحيات، وقت، أرشفة SMS)
```

## تدفّق البيانات

```
الإشعارات ─┐
SMS ───────┼──> InboxRepository ──> Room (Apps/Accounts/Messages) ──> Flow ──> Compose UI
IMAP ──────┘
```

## الأمان والخصوصية

- جميع البيانات تبقى على الجهاز (لا خوادم خارجية).
- رموز OAuth تُخزَّن مشفّرة عبر `EncryptedSharedPreferences` ولا تُنسخ خلال الـ backup.
- لا تُخزَّن كلمات مرور حسابات Google إطلاقاً.
