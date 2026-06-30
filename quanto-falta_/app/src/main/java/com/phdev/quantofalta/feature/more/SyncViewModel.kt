package com.phdev.quantofalta.feature.more

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.SyncOperationEntity
import com.phdev.quantofalta.core.database.SyncOperationStatus
import java.util.UUID
import com.phdev.quantofalta.core.sync.SyncWorker
import com.phdev.quantofalta.billing.EntitlementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.phdev.quantofalta.feature.premiumticket.PremiumTicketClient

sealed class SyncUiState {
    object Initial : SyncUiState()
    object Loading : SyncUiState()
    object OtpRequested : SyncUiState()
    object Authenticated : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncMillis: Long? = null,
    val lastError: String? = null,
)

class SyncViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val appDatabase: AppDatabase,
    private val entitlementManager: EntitlementManager
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("sync_prefs", 0)

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Initial)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _syncStatus = MutableStateFlow(
        SyncStatus(lastSyncMillis = prefs.getLong("last_sync_millis", -1L).takeIf { it > 0 })
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _hasUnreadTicket = MutableStateFlow(false)
    val hasUnreadTicket: StateFlow<Boolean> = _hasUnreadTicket.asStateFlow()

    init {
        checkAuthStatus()
        observeWorkManager()
        checkBadges()
    }

    fun checkBadges() {
        viewModelScope.launch {
            val client = PremiumTicketClient(getApplication())
            val status = client.getActiveTicketStatus()
            _hasUnreadTicket.value = status == "aguardando_usuario" || status == "token_enviado"
        }
    }

    private fun checkAuthStatus() {
        if (authManager.getAccessToken() != null) {
            _uiState.value = SyncUiState.Authenticated
            authManager.getEmail()?.let { email ->
                entitlementManager.setSavedEmail(email)
                viewModelScope.launch {
                    entitlementManager.syncWithServer(getApplication())
                }
            }
        } else {
            _uiState.value = SyncUiState.Initial
        }
    }

    private fun observeWorkManager() {
        val workManager = WorkManager.getInstance(getApplication())

        // Observe the one-time manual sync job
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
            .onEach { workInfoList ->
                val info = workInfoList.firstOrNull() ?: return@onEach
                when (info.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        _syncStatus.value = _syncStatus.value.copy(
                            isSyncing = true,
                            lastError = null
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val lastSync = info.outputData.getLong("last_sync", 0L)
                            .takeIf { it > 0L }
                            ?: System.currentTimeMillis()
                        prefs.edit().putLong("last_sync_millis", lastSync).apply()
                        _syncStatus.value = SyncStatus(
                            isSyncing = false,
                            lastSyncMillis = lastSync,
                            lastError = null
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = info.outputData.getString("error")
                            ?: "Falha desconhecida. Tente novamente."
                        _syncStatus.value = _syncStatus.value.copy(
                            isSyncing = false,
                            lastError = error
                        )
                    }
                    WorkInfo.State.CANCELLED -> {
                        _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
                    }
                    else -> {
                        // BLOCKED or other states — keep previous status
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
    }

    fun requestOtp() {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value.trim()).matches()) {
            _uiState.value = SyncUiState.Error("E-mail inválido")
            return
        }
        _email.value = _email.value.trim()
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
        val normalizedCode = code.trim()
        if (
            !android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value.trim()).matches() ||
            normalizedCode.length != 6 ||
            normalizedCode.any { !it.isDigit() }
        ) {
            _uiState.value = SyncUiState.Error("Informe um código válido de 6 dígitos.")
            return
        }
        _uiState.value = SyncUiState.Loading
        viewModelScope.launch {
            val result = authManager.verifyOtp(_email.value.trim(), normalizedCode)
            if (result.isSuccess) {
                val verifiedEmail = _email.value.trim()
                // Revoke any entitlements from a previous account before associating
                // the new email. This prevents premium from leaking between accounts
                // during the window between setSavedEmail and the server sync response.
                entitlementManager.revokeNonPlayStoreEntitlements()
                entitlementManager.setSavedEmail(verifiedEmail)
                entitlementManager.syncWithServer(getApplication())

                // On first connect, enqueue all local events to be synced to the cloud
                if (authManager.getSyncCursor().isEmpty()) {
                    val allEvents = appDatabase.eventDao().getAllEventsSync()
                    val syncEntries = allEvents.map {
                        SyncOperationEntity(
                            UUID.randomUUID().toString(),
                            "card",
                            it.id,
                            "UPDATE",
                            null,
                            SyncOperationStatus.PENDING
                        )
                    }
                    appDatabase.syncOperationDao().insertAll(syncEntries)
                }

                _uiState.value = SyncUiState.Authenticated
                syncNow() // Trigger initial sync
            } else {
                _uiState.value = SyncUiState.Error(result.exceptionOrNull()?.message ?: "Código inválido")
            }
        }
    }

    fun syncNow() {
        // Optimistically set syncing = true right away for instant feedback
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, lastError = null)
        SyncWorker.enqueue(getApplication())
    }

    fun restoreEvents() {
        viewModelScope.launch {
            // Force full restore: reset cursor and trigger sync
            authManager.saveSyncCursor("")
            syncNow()
        }
    }

    fun clearError() {
        _syncStatus.value = _syncStatus.value.copy(lastError = null)
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = SyncUiState.Loading
            authManager.logout()
            // Immediately drop any server-granted premium so the state
            // does not linger until the 7-day offline TTL expires.
            // Google Play purchases (play_store_*) are device-tied and preserved.
            entitlementManager.revokeNonPlayStoreEntitlements()
            _syncStatus.value = SyncStatus()
            prefs.edit().remove("last_sync_millis").apply()
            _uiState.value = SyncUiState.Initial
        }
    }
}
