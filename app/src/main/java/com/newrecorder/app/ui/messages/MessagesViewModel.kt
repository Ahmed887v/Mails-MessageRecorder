package com.newrecorder.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.newrecorder.app.data.local.entity.MessageEntity
import com.newrecorder.app.data.repository.InboxRepository
import com.newrecorder.app.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: InboxRepository,
    private val accountId: Long
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> =
        repository.observeMessages(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markAllRead() {
        viewModelScope.launch { repository.markAccountRead(accountId) }
    }

    fun markRead(messageId: Long) {
        viewModelScope.launch { repository.markMessageRead(messageId) }
    }

    companion object {
        fun factory(accountId: Long) = viewModelFactory {
            initializer { MessagesViewModel(appContainer.repository, accountId) }
        }
    }
}
