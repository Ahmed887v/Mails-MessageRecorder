package com.newrecorder.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.newrecorder.app.data.local.entity.AccountEntity
import com.newrecorder.app.data.repository.InboxRepository
import com.newrecorder.app.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AccountsViewModel(
    repository: InboxRepository,
    appPackage: String
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> =
        repository.observeAccounts(appPackage)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(appPackage: String) = viewModelFactory {
            initializer { AccountsViewModel(appContainer.repository, appPackage) }
        }
    }
}
