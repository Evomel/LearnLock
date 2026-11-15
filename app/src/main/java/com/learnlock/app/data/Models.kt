package com.learnlock.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnlock.taskengine.TaskCategory

@Entity(tableName = "child_profile")
data class ChildProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Ребёнок",
    val ageYears: Int = 8,
    val difficultyOffset: Int = 0,
    val sessionMinutes: Int = 15,
    val activeWindowStartHour: Int = 7,
    val activeWindowEndHour: Int = 21,
    val outsideWindowMode: String = OutsideWindowMode.NO_TASKS.name,
    val mathEnabled: Boolean = true,
    val readingEnabled: Boolean = true,
    val logicEnabled: Boolean = true,
    val mathWeight: Int = 1,
    val readingWeight: Int = 1,
    val logicWeight: Int = 1,
    val maxWrongBeforeEasier: Int = 3,
    val hintsEnabled: Boolean = true,
    val lockEnabled: Boolean = false
)

enum class OutsideWindowMode {
    NO_TASKS,
    HARD_BLOCK
}

@Entity(tableName = "parent_auth")
data class ParentAuthEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val salt: String
)

@Entity(tableName = "task_attempt")
data class TaskAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val category: String,
    val correct: Boolean,
    val usedHint: Boolean,
    val usedParentUnlock: Boolean,
    val difficultyLevel: Int,
    val skillTag: String = ""
)

data class SessionState(
    val unlockedUntilMillis: Long = 0L,
    val consecutiveWrong: Int = 0,
    val lastAccuracyWindow: Double = 0.7
)

fun ChildProfileEntity.enabledCategories(): Set<TaskCategory> = buildSet {
    if (mathEnabled) add(TaskCategory.MATH)
    if (readingEnabled) add(TaskCategory.READING)
    if (logicEnabled) add(TaskCategory.LOGIC)
}

fun ChildProfileEntity.categoryWeights(): Map<TaskCategory, Int> = mapOf(
    TaskCategory.MATH to mathWeight,
    TaskCategory.READING to readingWeight,
    TaskCategory.LOGIC to logicWeight
)
