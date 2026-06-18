package com.phdev.quantofalta.feature.more

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.AppContainer
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SyncUiState {
    object Initial : SyncUiState()
    object Loading : SyncUiState()
    object OtpRequested : SyncUiState()
    object Authenticated : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}

class SyncViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val appDatabase: AppDatabase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Initial)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        if (authManager.getAccessToken() != null) {
            _uiState.value = SyncUiState.Authenticated
        } else {
            _uiState.value = SyncUiState.Initial
        }
    }

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
    }

    fun requestOtp() {
        if (_email.value.isBlank()) {
            _uiState.value = SyncUiState.Error("E-mail inválido")
            return
        }
        _uiState.value = SyncUiState.Loading
        viewModelScope.launch {
            val result = authManager.requestOtp(_email.value)
            if (result.isSuccess) {
                _uiState.value = SyncUiState.OtpRequested
            } else {
                _uiState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "Erro desconhecido")
            }
        }
    }

    fun verifyOtp(code: String) {
        if (code.isBlank()) return
        _uiState.value = SyncUiState.Loading
        viewModelScope.launch {
            val result = authManager.verifyOtp(_email.value, code)
            if (result.isSuccess) {
                // Phase 12: Data Migration
                // On first connect, enqueue all local events to be synced to the cloud
                if (authManager.getSyncCursor().isEmpty()) {
                    val allEvents = appDatabase.eventDao().getAllEventsSync()
                    val outboxEntries = allEvents.map { 
                        com.phdev.quantofalta.core.database.OutboxEntity(it.id, "u", it.localRevision, System.currentTimeMillis()) 
                    }
                    appDatabase.outboxDao().insertAll(outboxEntries)
                }

                _uiState.value = SyncUiState.Authenticated
                syncNow() // Trigger initial sync
            } else {
                _uiState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "Código inválido")
            }
        }
    }

    fun syncNow() {
        SyncWorker.enqueue(getApplication())
    }

    fun restoreEvents() {
        viewModelScope.launch {
            // Force full restore: reset cursor and trigger sync
            authManager.saveSyncCursor("")
            SyncWorker.enqueue(getApplication())
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = SyncUiState.Loading
            authManager.logout()
            _uiState.value = SyncUiState.Initial
        }
    }
}
