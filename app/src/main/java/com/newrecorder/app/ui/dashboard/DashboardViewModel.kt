package com.newrecorder.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.newrecorder.app.data.local.entity.AppWithAccounts
import com.newrecorder.app.data.repository.InboxRepository
import com.newrecorder.app.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val apps: List<AppWithAccounts> = emptyList(),
    val unreadByAccount: Map<Long, Int> = emptyMap()
) {
    fun unreadOf(accountId: Long): Int = unreadByAccount[accountId] ?: 0
}

class DashboardViewModel(repository: InboxRepository) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        repository.observeTree(),
        repository.observeUnreadCounts()
    ) { apps, counts ->
        DashboardUiState(apps, counts.associate { it.accountId to it.count })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { DashboardViewModel(appContainer.repository) }
        }
    }
}
