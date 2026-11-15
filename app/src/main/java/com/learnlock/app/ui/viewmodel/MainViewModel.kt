package com.learnlock.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learnlock.app.data.ChildProfileEntity
import com.learnlock.app.data.LearnLockRepository
import com.learnlock.app.data.SessionState
import com.learnlock.app.data.TaskAttemptEntity
import com.learnlock.app.lock.LockController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MainUiState(
    val profile: ChildProfileEntity = ChildProfileEntity(),
    val session: SessionState = SessionState(),
    val hasParentPin: Boolean = false,
    val attempts: List<TaskAttemptEntity> = emptyList(),
    val bootstrapped: Boolean = false
)

class MainViewModel(
    application: Application,
    private val repository: LearnLockRepository
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(MainUiState())
        private set

    fun bootstrap() {
        viewModelScope.launch {
            repository.ensureDefaults()
            uiState = uiState.copy(hasParentPin = repository.hasParentPin(), bootstrapped = true)
            launch {
                repository.observeProfile().collectLatest { profile ->
                    if (profile != null) uiState = uiState.copy(profile = profile)
                }
            }
            launch {
                repository.observeSession().collectLatest { session ->
                    uiState = uiState.copy(session = session)
                }
            }
            launch {
                repository.observeStats(7).collectLatest { attempts ->
                    uiState = uiState.copy(attempts = attempts)
                }
            }
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            repository.setParentPin(pin)
            uiState = uiState.copy(hasParentPin = true)
        }
    }

    fun saveProfile(profile: ChildProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            if (profile.lockEnabled) {
                LockController.startMonitoring(getApplication())
            } else {
                LockController.stopMonitoring(getApplication())
            }
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        saveProfile(uiState.profile.copy(lockEnabled = enabled))
    }

    fun previewLock() {
        LockController.presentLock(getApplication(), forcePreview = true)
    }

    fun endSession() {
        viewModelScope.launch { repository.endSessionNow() }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: LearnLockRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
