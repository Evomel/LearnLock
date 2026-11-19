package com.learnlock.app.data

import com.learnlock.taskengine.AttemptRecord
import com.learnlock.taskengine.LearnerStats
import com.learnlock.taskengine.SkillTag
import com.learnlock.taskengine.Task
import com.learnlock.taskengine.TaskCategory
import com.learnlock.taskengine.TaskEngine
import com.learnlock.taskengine.TaskRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Фасад доменной логики LearnLock для UI и lock-слоя.
 * Связывает профиль, сессию, статистику и [TaskEngine].
 */
class LearnLockRepository(
    private val profileDao: ProfileDao,
    private val parentAuthDao: ParentAuthDao,
    private val statsDao: StatsDao,
    private val sessionStore: SessionStore,
    private val taskEngine: TaskEngine
) {
    fun observeProfile(): Flow<ChildProfileEntity?> = profileDao.observeProfile()
    fun observeSession(): Flow<SessionState> = sessionStore.state

    /** Попытки за последние [days] дней (экран статистики). */
    fun observeStats(days: Int): Flow<List<TaskAttemptEntity>> {
        val from = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return statsDao.observeSince(from)
    }

    /** Создаёт профиль по умолчанию, если БД пуста. */
    suspend fun ensureDefaults() {
        if (profileDao.getProfile() == null) {
            profileDao.upsert(ChildProfileEntity())
        }
    }

    suspend fun hasParentPin(): Boolean = parentAuthDao.get() != null

    /** Сохраняет новый родительский PIN (4–12 цифр) как PBKDF2-хеш. */
    suspend fun setParentPin(pin: String) {
        require(pin.length in 4..12) { "PIN must be 4-12 digits" }
        val salt = PinHasher.createSalt()
        parentAuthDao.upsert(
            ParentAuthEntity(
                pinHash = PinHasher.hash(pin, salt),
                salt = salt
            )
        )
    }

    /** Проверяет введённый PIN родителя. */
    suspend fun verifyParentPin(pin: String): ParentPinVerification {
        val entity = parentAuthDao.get() ?: return ParentPinVerification(valid = false)
        val now = System.currentTimeMillis()
        if (entity.lockedUntilMillis > now) {
            return ParentPinVerification(valid = false, lockedUntilMillis = entity.lockedUntilMillis)
        }
        if (PinHasher.matches(pin, entity)) {
            parentAuthDao.upsert(entity.copy(failedAttempts = 0, lockedUntilMillis = 0L))
            return ParentPinVerification(valid = true)
        }
        val attempts = entity.failedAttempts + 1
        val lockedUntil = if (attempts >= 3) now + TimeUnit.SECONDS.toMillis(30) else 0L
        parentAuthDao.upsert(
            entity.copy(
                failedAttempts = if (lockedUntil > 0L) 0 else attempts,
                lockedUntilMillis = lockedUntil
            )
        )
        return ParentPinVerification(valid = false, lockedUntilMillis = lockedUntil)
    }

    suspend fun clearParentPin() = parentAuthDao.delete()

    suspend fun saveProfile(profile: ChildProfileEntity) {
        profileDao.upsert(profile)
    }

    /** Рассчитывает момент следующего запуска после сохранения настроек или PIN. */
    suspend fun scheduleNextLaunch(profile: ChildProfileEntity? = null) {
        val currentProfile = profile ?: getProfileOrDefault()
        val intervalMinutes = currentProfile.launchIntervalMinutes.coerceAtLeast(5)
        val nextLaunchAt = System.currentTimeMillis() +
            TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
        sessionStore.setUnlockedUntil(nextLaunchAt)
        sessionStore.setNextLaunchAt(nextLaunchAt)
        sessionStore.setConsecutiveWrong(0)
    }

    suspend fun refreshNextLaunchIfExpired() {
        val session = sessionStore.state.first()
        if (session.nextLaunchAtMillis == 0L || session.nextLaunchAtMillis <= System.currentTimeMillis()) {
            scheduleNextLaunch()
        }
    }

    suspend fun getProfileOrDefault(): ChildProfileEntity =
        profileDao.getProfile() ?: ChildProfileEntity().also { profileDao.upsert(it) }

    /** true, если сейчас действует свободный доступ после решения задачи. */
    suspend fun isSessionActive(): Boolean {
        val state = sessionStore.state.first()
        return state.unlockedUntilMillis > System.currentTimeMillis()
    }

    /**
     * Нужно ли показать экран задачи после пробуждения / смены приложения.
     *
     * Условия отказа: блокировка выключена, сессия ещё активна,
     * либо сейчас вне окна и режим [OutsideWindowMode.NO_TASKS].
     */
    suspend fun shouldPresentLock(): Boolean {
        val profile = getProfileOrDefault()
        if (!profile.lockEnabled) return false
        if (isSessionActive()) return false
        return true
    }

    /** Генерирует следующую задачу с учётом возраста, точности и слабых навыков. */
    suspend fun nextTask(): Task {
        val profile = getProfileOrDefault()
        val session = sessionStore.state.first()
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val accuracy = statsDao.accuracySince(weekAgo) ?: session.lastAccuracyWindow
        val forceEasier = session.consecutiveWrong >= profile.maxWrongBeforeEasier
        val learnerStats = loadLearnerStats()
        return taskEngine.nextTask(
            TaskRequest(
                ageYears = profile.ageYears,
                difficultyOffset = profile.difficultyOffset,
                enabledCategories = profile.enabledCategories().ifEmpty {
                    setOf(TaskCategory.MATH)
                },
                categoryWeights = profile.categoryWeights(),
                recentAccuracy = accuracy,
                forceEasier = forceEasier,
                learnerStats = learnerStats
            )
        )
    }

    /**
     * Строит [LearnerStats] из последних попыток для AdaptiveSelector.
     * @param limit сколько последних учебных попыток учесть
     */
    suspend fun loadLearnerStats(limit: Int = 30): LearnerStats {
        val attempts = statsDao.recentLearningAttempts(limit)
        val records = attempts.mapNotNull { entity ->
            val skill = entity.skillTag.takeIf { it.isNotBlank() }
                ?.let { runCatching { SkillTag.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            val category = runCatching { TaskCategory.valueOf(entity.category) }.getOrNull()
                ?: skill.category
            AttemptRecord(
                skillTag = skill,
                category = category,
                correct = entity.correct
            )
        }
        return LearnerStats.fromAttempts(records)
    }

    /** Верный ответ: открывает сессию на [ChildProfileEntity.sessionMinutes] минут. */
    suspend fun onCorrectAnswer(task: Task, usedHint: Boolean) {
        val profile = getProfileOrDefault()
        val now = System.currentTimeMillis()
        val until = now + TimeUnit.MINUTES.toMillis(profile.accessDurationMinutes.coerceAtLeast(1).toLong())
        val nextLaunch = maxOf(
            until,
            now + TimeUnit.MINUTES.toMillis(profile.launchIntervalMinutes.coerceAtLeast(5).toLong())
        )
        sessionStore.setUnlockedUntil(until)
        sessionStore.setNextLaunchAt(nextLaunch)
        sessionStore.setConsecutiveWrong(0)
        insertAttempt(task, correct = true, usedHint = usedHint, usedParentUnlock = false)
        refreshAccuracy()
    }

    /** Неверный ответ: увеличивает счётчик ошибок, сессию не открывает. */
    suspend fun onWrongAnswer(task: Task, usedHint: Boolean) {
        val session = sessionStore.state.first()
        sessionStore.setConsecutiveWrong(session.consecutiveWrong + 1)
        insertAttempt(task, correct = false, usedHint = usedHint, usedParentUnlock = false)
        refreshAccuracy()
    }

    /**
     * Emergency-разблокировка родителем.
     * @return true, если PIN верен и сессия открыта
     */
    suspend fun unlockWithParentPin(pin: String): Boolean {
        if (!verifyParentPin(pin).valid) return false
        val profile = getProfileOrDefault()
        val now = System.currentTimeMillis()
        val until = now + TimeUnit.MINUTES.toMillis(profile.accessDurationMinutes.coerceAtLeast(1).toLong())
        val nextLaunch = maxOf(
            until,
            now + TimeUnit.MINUTES.toMillis(profile.launchIntervalMinutes.coerceAtLeast(5).toLong())
        )
        sessionStore.setUnlockedUntil(until)
        sessionStore.setNextLaunchAt(nextLaunch)
        sessionStore.setConsecutiveWrong(0)
        statsDao.insert(
            TaskAttemptEntity(
                timestampMillis = System.currentTimeMillis(),
                category = "PARENT",
                correct = true,
                usedHint = false,
                usedParentUnlock = true,
                difficultyLevel = 0,
                skillTag = ""
            )
        )
        return true
    }

    /** Принудительно сбрасывает свободную сессию. */
    suspend fun endSessionNow() {
        sessionStore.clearUnlock()
    }

    private suspend fun insertAttempt(
        task: Task,
        correct: Boolean,
        usedHint: Boolean,
        usedParentUnlock: Boolean
    ) {
        statsDao.insert(
            TaskAttemptEntity(
                timestampMillis = System.currentTimeMillis(),
                category = task.category.name,
                correct = correct,
                usedHint = usedHint,
                usedParentUnlock = usedParentUnlock,
                difficultyLevel = task.difficultyLevel,
                skillTag = task.skillTag.name
            )
        )
    }

    private suspend fun refreshAccuracy() {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val accuracy = statsDao.accuracySince(weekAgo) ?: 0.7
        sessionStore.setAccuracy(accuracy)
    }

    /**
     * Решение по текущему часу суток относительно окна профиля.
     * Поддерживает окно через полночь (start > end).
     */
    private fun outsideWindowDecision(profile: ChildProfileEntity): OutsideDecision {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = profile.activeWindowStartHour
        val end = profile.activeWindowEndHour
        val inWindow = if (start <= end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
        if (inWindow) return OutsideDecision.SHOW_TASK
        return when (OutsideWindowMode.valueOf(profile.outsideWindowMode)) {
            OutsideWindowMode.NO_TASKS -> OutsideDecision.ALLOW_FREE
            OutsideWindowMode.HARD_BLOCK -> OutsideDecision.HARD_BLOCK
        }
    }

    private enum class OutsideDecision {
        SHOW_TASK,
        ALLOW_FREE,
        HARD_BLOCK
    }
}
