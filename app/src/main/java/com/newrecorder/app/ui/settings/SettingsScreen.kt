package com.newrecorder.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newrecorder.app.ui.components.CardShape
import com.newrecorder.app.util.PermissionUtils
import com.newrecorder.app.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    authViewModel: GmailAuthViewModel = viewModel(factory = GmailAuthViewModel.Factory)
) {
    val context = LocalContext.current
    val state by settingsViewModel.state.collectAsStateWithLifecycle()
    val authMessage by authViewModel.message.collectAsStateWithLifecycle()
    val smsImported by settingsViewModel.smsImported.collectAsStateWithLifecycle()

    var listenerEnabled by remember { mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)
                batteryIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context)
                settingsViewModel.refreshGmail()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val gmailAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            // معالجة الاستجابة تتم داخل الـ ViewModel.
            authViewModel.handleResult(data)
            settingsViewModel.refreshGmail()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard(title = "صلاحيات النظام") {
                    PermissionRow(
                        title = "الوصول إلى الإشعارات",
                        subtitle = "يلزم لقراءة تنبيهات WhatsApp والماسنجر وغيرها",
                        granted = listenerEnabled,
                        onAction = { context.startActivity(PermissionUtils.notificationListenerSettingsIntent()) }
                    )
                    PermissionRow(
                        title = "إشعارات التطبيق",
                        subtitle = "السماح بإظهار إشعار الخدمة الدائمة",
                        granted = PermissionUtils.isPostNotificationsGranted(context),
                        onAction = { permissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)) }
                    )
                    PermissionRow(
                        title = "تجاهل تحسين البطارية",
                        subtitle = "لمنع النظام من تنويم التطبيق وإيقاف الجلب",
                        granted = batteryIgnored,
                        onAction = {
                            runCatching {
                                context.startActivity(PermissionUtils.requestIgnoreBatteryOptimizationIntent(context))
                            }.onFailure {
                                context.startActivity(PermissionUtils.batteryOptimizationSettingsIntent())
                            }
                        }
                    )
                    PermissionRow(
                        title = "استقبال رسائل SMS",
                        subtitle = "مطلوب لالتقاط الرسائل النصية لحظة وصولها",
                        granted = PermissionUtils.isReceiveSmsGranted(context),
                        onAction = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECEIVE_SMS,
                                    android.Manifest.permission.READ_SMS
                                )
                            )
                        }
                    )
                    PermissionRow(
                        title = "قراءة أرشيف SMS",
                        subtitle = "لاستيراد الرسائل النصية الحالية من الجهاز",
                        granted = PermissionUtils.isReadSmsGranted(context),
                        onAction = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECEIVE_SMS,
                                    android.Manifest.permission.READ_SMS
                                )
                            )
                        }
                    )
                    OutlinedButton(
                        onClick = { settingsViewModel.importSmsArchive() },
                        enabled = PermissionUtils.isReadSmsGranted(context)
                    ) { Text("استيراد رسائل SMS الحالية") }
                    if (smsImported != null) {
                        Text(
                            text = "تم استيراد $smsImported رسالة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                SectionCard(title = "تشخيص التقاط الإشعارات") {
                    val diag = state.diagnostics
                    Text(
                        text = if (listenerEnabled) {
                            "خدمة الإشعارات مفعّلة. أرسل رسالة تجريبية من واتساب وستظهر القيم أدناه خلال ثوانٍ."
                        } else {
                            "خدمة الإشعارات غير مفعّلة — لن يُلتقط أي شيء. فعّلها من \"الوصول إلى الإشعارات\" أعلاه."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (listenerEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    DiagnosticRow(
                        label = "آخر تطبيق أرسل إشعاراً",
                        value = diag.lastSeenPackage.ifBlank { "لا شيء بعد" }
                    )
                    DiagnosticRow(
                        label = "وقت آخر إشعار",
                        value = if (diag.lastSeenAt > 0) TimeFormat.relative(diag.lastSeenAt) else "—"
                    )
                    DiagnosticRow(
                        label = "رسائل حُوِّلت إلى الصندوق",
                        value = diag.capturedCount.toString()
                    )
                    TextButton(onClick = { settingsViewModel.resetCaptureDiagnostics() }) {
                        Text("تصفير العدّاد")
                    }
                }
            }

            item {
                SectionCard(title = "حسابات Gmail") {
                    if (state.gmailAccounts.isEmpty()) {
                        Text(
                            text = "لا توجد حسابات مرتبطة بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.gmailAccounts.forEach { email ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(email, modifier = Modifier.weight(1f))
                                TextButton(onClick = { settingsViewModel.removeGmail(email) }) {
                                    Text("فصل")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { gmailAuthLauncher.launch(authViewModel.authRequestIntent()) }) {
                        Text("تسجيل الدخول باستخدام جوجل")
                    }
                    if (authMessage != null) {
                        Text(
                            text = authMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            item {
                SectionCard(title = "خدمة الخلفية") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الخدمة المستمرة", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "إبقاء الجلب والتقاط الإشعارات يعمل في الخلفية",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.keepAlive,
                            onCheckedChange = { settingsViewModel.setKeepAlive(it) }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("فترة مزامنة البريد", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 15, 30, 60).forEach { minutes ->
                            FilterChip(
                                selected = state.syncIntervalMinutes == minutes,
                                onClick = { settingsViewModel.setSyncInterval(minutes) },
                                label = { Text("$minutes د") }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "حول التطبيق") {
                    Text(
                        text = "NewRecorder 1.0.0 — مدير صندوق وارد موحّد يعمل محلياً بالكامل. " +
                            "جميع البيانات تُخزَّن على جهازك فقط.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            Text(
                text = "مفعّلة",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            OutlinedButton(onClick = onAction) { Text("تفعيل") }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Suppress("unused")
private fun Context.noop() = Unit
