package com.rekosuo.gymtracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekosuo.gymtracker.data.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupMessage {
    data class Success(val text: String) : BackupMessage()
    data class Error(val text: String) : BackupMessage()
}

data class BackupState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null,
)

sealed class BackupEvent {
    data class ExportTo(val uri: Uri) : BackupEvent()
    data class ImportFrom(val uri: Uri) : BackupEvent()
    object DismissMessage : BackupEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun onEvent(event: BackupEvent) {
        when (event) {
            is BackupEvent.ExportTo -> runExport(event.uri)
            is BackupEvent.ImportFrom -> runImport(event.uri)
            is BackupEvent.DismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun runExport(uri: Uri) {
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val result = repository.exportTo(uri)
            _state.update {
                it.copy(
                    isBusy = false,
                    message = result.fold(
                        onSuccess = { BackupMessage.Success("Data exported") },
                        onFailure = { e -> BackupMessage.Error(e.message ?: "Export failed") }
                    )
                )
            }
        }
    }

    private fun runImport(uri: Uri) {
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val result = repository.importFrom(uri)
            _state.update {
                it.copy(
                    isBusy = false,
                    message = result.fold(
                        onSuccess = { BackupMessage.Success("Data imported") },
                        onFailure = { e -> BackupMessage.Error(e.message ?: "Import failed") }
                    )
                )
            }
        }
    }
}
