package com.learnlock.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learnlock.taskengine.TaskCategory

/**
 * Профиль ребёнка и настройки LearnLock (одна строка в БД).
 *
 * [sessionMinutes] — длительность свободного доступа после верного ответа,
 * а не таймер до появления блокировки.
 *
 * [activeWindowStartHour]/[activeWindowEndHour] — часы, когда задачи показываются;
 * вне окна поведение задаёт [outsideWindowMode].
 */
@Entity(tableName = "child_profile")
data class ChildProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Ребёнок",
    val ageYears: Int = 8,
    /** Смещение сложности: −1 легче, 0 норма, +1 сложнее. */
    val difficultyOffset: Int = 0,
    /** Минуты свободного пользования после решения / parent PIN. */
    val sessionMinutes: Int = 5,
    /** Вариант фона главного и экранов настроек. */
    val backgroundColor: Int = 0,
    val activeWindowStartHour: Int = 7,
    val activeWindowEndHour: Int = 21,
    val outsideWindowMode: String = OutsideWindowMode.NO_TASKS.name,
    val mathEnabled: Boolean = true,
    val readingEnabled: Boolean = true,
    val logicEnabled: Boolean = true,
    val mathWeight: Int = 1,
    val readingWeight: Int = 1,
    val logicWeight: Int = 1,
    /** После стольких ошибок подряд генератор упрощает задачу. */
    val maxWrongBeforeEasier: Int = 3,
    val hintsEnabled: Boolean = true,
    /** Главный рубильник: слушать сон/пробуждение и показывать задачи. */
    val lockEnabled: Boolean = false
)

/**
 * Поведение вне активного окна часов.
 * - [NO_TASKS] — телефон свободен без задач
 * - [HARD_BLOCK] — всё равно требовать задачу (жёсткий режим)
 */
enum class OutsideWindowMode {
    NO_TASKS,
    HARD_BLOCK
}

/** Соль + PBKDF2-хеш родительского PIN. */
@Entity(tableName = "parent_auth")
data class ParentAuthEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val salt: String
)

/** Одна попытка решения (или parent unlock) для статистики и адаптации. */
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

/**
 * Эфемерное состояние сессии (DataStore, не Room).
 * @property unlockedUntilMillis до какого момента не показывать задачу
 * @property consecutiveWrong подряд ошибок (для упрощения)
 * @property lastAccuracyWindow кэш точности за неделю
 */
data class SessionState(
    val unlockedUntilMillis: Long = 0L,
    val nextLaunchAtMillis: Long = 0L,
    val consecutiveWrong: Int = 0,
    val lastAccuracyWindow: Double = 0.7
)

/** Включённые категории задач по флагам профиля. */
fun ChildProfileEntity.enabledCategories(): Set<TaskCategory> = buildSet {
    if (mathEnabled) add(TaskCategory.MATH)
    if (readingEnabled) add(TaskCategory.READING)
    if (logicEnabled) add(TaskCategory.LOGIC)
}

/** Веса категорий для взвешенного выбора провайдера. */
fun ChildProfileEntity.categoryWeights(): Map<TaskCategory, Int> = mapOf(
    TaskCategory.MATH to mathWeight,
    TaskCategory.READING to readingWeight,
    TaskCategory.LOGIC to logicWeight
)
