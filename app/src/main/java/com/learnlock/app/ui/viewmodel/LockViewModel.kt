package com.learnlock.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learnlock.app.data.ChildProfileEntity
import com.learnlock.app.data.LearnLockRepository
import com.learnlock.app.lock.LockController
import com.learnlock.taskengine.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Состояние экрана задачи ([com.learnlock.app.ui.screens.LockTaskScreen]). */
data class LockUiState(
    val loading: Boolean = true,
    val profile: ChildProfileEntity = ChildProfileEntity(),
    val task: Task? = null,
    val feedback: String? = null,
    val hintVisible: Boolean = false,
    val hintUsed: Boolean = false,
    /** Пауза после ошибки, сек. */
    val cooldownSeconds: Int = 0,
    val showParentPin: Boolean = false,
    val unlocked: Boolean = false
)

/**
 * ViewModel экрана блокировки: загрузка задачи, проверка ответа,
 * подсказка и parent unlock.
 */
class LockViewModel(
    private val application: Application,
    private val repository: LearnLockRepository
) : ViewModel() {
    var uiState by mutableStateOf(LockUiState())
        private set

    /**
     * Загружает задачу.
     * @param forcePreview если true — сбрасывает сессию и всегда показывает задачу
     */
    fun load(forcePreview: Boolean = false) {
        viewModelScope.launch {
            val profile = repository.getProfileOrDefault()
            // Preview с главного экрана всегда показывает задачу.
            if (!forcePreview && repository.isSessionActive()) {
                uiState = uiState.copy(loading = false, unlocked = true, profile = profile)
                return@launch
            }
            if (forcePreview) {
                repository.endSessionNow()
            }
            val task = repository.nextTask()
            uiState = uiState.copy(
                loading = false,
                unlocked = false,
                profile = profile,
                task = task,
                feedback = if (forcePreview) "Режим проверки задачи" else null
            )
        }
    }

    /** Показывает подсказку один раз за задачу (если разрешено в профиле). */
    fun useHint() {
        val task = uiState.task ?: return
        if (!uiState.profile.hintsEnabled || uiState.hintUsed) return
        uiState = uiState.copy(
            hintUsed = true,
            hintVisible = true,
            feedback = task.hint ?: "Подумай ещё"
        )
    }

    /**
     * Проверяет ответ. При успехе открывает сессию;
     * при ошибке — короткий cooldown и новая задача.
     */
    fun submitAnswer(raw: String) {
        val task = uiState.task ?: return
        if (uiState.cooldownSeconds > 0 || uiState.unlocked) return
        viewModelScope.launch {
            if (task.matches(raw)) {
                repository.onCorrectAnswer(task, uiState.hintUsed)
                LockController.scheduleNextLaunch(application)
                uiState = uiState.copy(
                    unlocked = true,
                    feedback = "Отлично! Телефон открыт на ${uiState.profile.accessDurationMinutes} мин."
                )
            } else {
                repository.onWrongAnswer(task, uiState.hintUsed)
                uiState = uiState.copy(feedback = "Неверно. Попробуй ещё раз.")
                for (sec in 2 downTo 1) {
                    uiState = uiState.copy(cooldownSeconds = sec)
                    delay(1000)
                }
                val refreshed = repository.nextTask()
                uiState = uiState.copy(
                    task = refreshed,
                    hintVisible = false,
                    cooldownSeconds = 0
                )
            }
        }
    }

    /** Emergency unlock родительским PIN. */
    fun parentUnlock(pin: String) {
        viewModelScope.launch {
            val ok = repository.unlockWithParentPin(pin)
            if (ok) LockController.scheduleNextLaunch(application)
            uiState = if (ok) {
                uiState.copy(
                    unlocked = true,
                    feedback = "Разблокировано родителем",
                    showParentPin = false
                )
            } else {
                uiState.copy(feedback = "Неверный PIN")
            }
        }
    }

    fun toggleParentPin(show: Boolean) {
        uiState = uiState.copy(showParentPin = show)
    }
}

/** Фабрика [LockViewModel] с инъекцией репозитория. */
class LockViewModelFactory(
    private val application: Application,
    private val repository: LearnLockRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LockViewModel::class.java)) {
            return LockViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}
