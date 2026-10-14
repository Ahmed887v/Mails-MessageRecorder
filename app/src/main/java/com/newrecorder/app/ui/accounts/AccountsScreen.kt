package com.newrecorder.app.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newrecorder.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    appPackage: String,
    appTitle: String,
    onBack: () -> Unit,
    onOpenMessages: (Long) -> Unit,
    viewModel: AccountsViewModel = viewModel(
        factory = AccountsViewModel.factory(appPackage),
        key = appPackage
    )
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.AccountCircle,
                title = "لا توجد حسابات",
                subtitle = "يُكتشف الحساب تلقائياً فور وصول أول إشعار من هذا التطبيق. تأكد من تفعيل \"الوصول إلى الإشعارات\" في الإعدادات.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    ListItem(
                        headlineContent = { Text(account.displayName) },
                        supportingContent = {
                            if (account.isCloned) {
                                Text("حساب مزدوج (Cloned)")
                            } else {
                                Text("حساب أساسي")
                            }
                        },
                        trailingContent = {
                            Text(
                                text = "عرض الرسائل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onOpenMessages(account.id) }
                    )
                }
            }
        }
    }
}
