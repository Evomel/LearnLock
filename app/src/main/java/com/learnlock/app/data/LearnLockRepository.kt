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

class LearnLockRepository(
    private val profileDao: ProfileDao,
    private val parentAuthDao: ParentAuthDao,
    private val statsDao: StatsDao,
    private val sessionStore: SessionStore,
    private val taskEngine: TaskEngine
) {
    fun observeProfile(): Flow<ChildProfileEntity?> = profileDao.observeProfile()
    fun observeSession(): Flow<SessionState> = sessionStore.state

    fun observeStats(days: Int): Flow<List<TaskAttemptEntity>> {
        val from = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return statsDao.observeSince(from)
    }

    suspend fun ensureDefaults() {
        if (profileDao.getProfile() == null) {
            profileDao.upsert(ChildProfileEntity())
        }
    }

    suspend fun hasParentPin(): Boolean = parentAuthDao.get() != null

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

    suspend fun verifyParentPin(pin: String): Boolean {
        val entity = parentAuthDao.get() ?: return false
        return PinHasher.matches(pin, entity)
    }

    suspend fun saveProfile(profile: ChildProfileEntity) {
        profileDao.upsert(profile)
    }

    suspend fun getProfileOrDefault(): ChildProfileEntity =
        profileDao.getProfile() ?: ChildProfileEntity().also { profileDao.upsert(it) }

    suspend fun isSessionActive(): Boolean {
        val state = sessionStore.state.first()
        return state.unlockedUntilMillis > System.currentTimeMillis()
    }

    suspend fun shouldPresentLock(): Boolean {
        val profile = getProfileOrDefault()
        if (!profile.lockEnabled) return false
        if (isSessionActive()) return false
        return when (outsideWindowDecision(profile)) {
            OutsideDecision.ALLOW_FREE -> false
            OutsideDecision.SHOW_TASK, OutsideDecision.HARD_BLOCK -> true
        }
    }

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

    suspend fun onCorrectAnswer(task: Task, usedHint: Boolean) {
        val profile = getProfileOrDefault()
        val until = System.currentTimeMillis() +
            TimeUnit.MINUTES.toMillis(profile.sessionMinutes.toLong())
        sessionStore.setUnlockedUntil(until)
        sessionStore.setConsecutiveWrong(0)
        insertAttempt(task, correct = true, usedHint = usedHint, usedParentUnlock = false)
        refreshAccuracy()
    }

    suspend fun onWrongAnswer(task: Task, usedHint: Boolean) {
        val session = sessionStore.state.first()
        sessionStore.setConsecutiveWrong(session.consecutiveWrong + 1)
        insertAttempt(task, correct = false, usedHint = usedHint, usedParentUnlock = false)
        refreshAccuracy()
    }

    suspend fun unlockWithParentPin(pin: String): Boolean {
        if (!verifyParentPin(pin)) return false
        val profile = getProfileOrDefault()
        val until = System.currentTimeMillis() +
            TimeUnit.MINUTES.toMillis(profile.sessionMinutes.toLong())
        sessionStore.setUnlockedUntil(until)
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
