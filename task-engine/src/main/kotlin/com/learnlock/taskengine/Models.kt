package com.learnlock.taskengine

enum class TaskCategory {
    MATH,
    READING,
    LOGIC
}

enum class SkillTag {
    MATH_ADD,
    MATH_SUB,
    MATH_MUL,
    MATH_COMPARE,
    MATH_MIX,
    READ_LETTER,
    READ_ODD,
    READ_SYNONYM,
    LOGIC_SEQ,
    LOGIC_PATTERN,
    LOGIC_WORD;

    val category: TaskCategory
        get() = when (this) {
            MATH_ADD, MATH_SUB, MATH_MUL, MATH_COMPARE, MATH_MIX -> TaskCategory.MATH
            READ_LETTER, READ_ODD, READ_SYNONYM -> TaskCategory.READING
            LOGIC_SEQ, LOGIC_PATTERN, LOGIC_WORD -> TaskCategory.LOGIC
        }

    fun displayNameRu(): String = when (this) {
        MATH_ADD -> "Сложение"
        MATH_SUB -> "Вычитание"
        MATH_MUL -> "Умножение"
        MATH_COMPARE -> "Сравнение чисел"
        MATH_MIX -> "Смешанные примеры"
        READ_LETTER -> "Пропущенная буква"
        READ_ODD -> "Лишнее слово"
        READ_SYNONYM -> "Синонимы"
        LOGIC_SEQ -> "Последовательности"
        LOGIC_PATTERN -> "Узоры"
        LOGIC_WORD -> "Текстовые задачи"
    }
}

enum class AnswerInputType {
    NUMBER,
    CHOICE,
    TEXT
}

data class TaskChoice(
    val id: String,
    val label: String
)

data class Task(
    val id: String,
    val category: TaskCategory,
    val prompt: String,
    val inputType: AnswerInputType,
    val choices: List<TaskChoice> = emptyList(),
    val acceptedAnswers: List<String>,
    val hint: String? = null,
    val difficultyLevel: Int,
    val skillTag: SkillTag
) {
    fun matches(rawAnswer: String): Boolean {
        val normalized = normalize(rawAnswer)
        return acceptedAnswers.any { normalize(it) == normalized }
    }

    companion object {
        fun normalize(value: String): String =
            value.trim().lowercase().replace(',', '.')
    }
}

data class AttemptRecord(
    val skillTag: SkillTag,
    val category: TaskCategory,
    val correct: Boolean
)

data class LearnerStats(
    val overallAccuracy: Double = 0.7,
    val categoryAccuracy: Map<TaskCategory, Double> = emptyMap(),
    val skillAccuracy: Map<SkillTag, Double> = emptyMap(),
    val skillAttempts: Map<SkillTag, Int> = emptyMap()
) {
    fun weakSkills(minAttempts: Int = 3, threshold: Double = 0.55): List<SkillTag> =
        skillAccuracy
            .filter { (skill, acc) -> (skillAttempts[skill] ?: 0) >= minAttempts && acc < threshold }
            .entries
            .sortedBy { it.value }
            .map { it.key }

    fun strongSkills(minAttempts: Int = 3, threshold: Double = 0.85): List<SkillTag> =
        skillAccuracy
            .filter { (skill, acc) -> (skillAttempts[skill] ?: 0) >= minAttempts && acc >= threshold }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

    companion object {
        fun fromAttempts(attempts: List<AttemptRecord>): LearnerStats {
            if (attempts.isEmpty()) return LearnerStats()
            val overall = attempts.count { it.correct }.toDouble() / attempts.size
            val categoryAccuracy = attempts
                .groupBy { it.category }
                .mapValues { (_, list) -> list.count { it.correct }.toDouble() / list.size }
            val bySkill = attempts.groupBy { it.skillTag }
            val skillAccuracy = bySkill.mapValues { (_, list) ->
                list.count { it.correct }.toDouble() / list.size
            }
            val skillAttempts = bySkill.mapValues { it.value.size }
            return LearnerStats(
                overallAccuracy = overall,
                categoryAccuracy = categoryAccuracy,
                skillAccuracy = skillAccuracy,
                skillAttempts = skillAttempts
            )
        }
    }
}

data class TaskRequest(
    val ageYears: Int,
    val difficultyOffset: Int = 0,
    val enabledCategories: Set<TaskCategory> = TaskCategory.entries.toSet(),
    val categoryWeights: Map<TaskCategory, Int> = mapOf(
        TaskCategory.MATH to 1,
        TaskCategory.READING to 1,
        TaskCategory.LOGIC to 1
    ),
    val recentAccuracy: Double = 0.7,
    val forceEasier: Boolean = false,
    val preferredCategory: TaskCategory? = null,
    val preferredSkill: SkillTag? = null,
    val learnerStats: LearnerStats? = null
)

data class DifficultyProfile(
    val ageYears: Int,
    val offset: Int,
    val forceEasier: Boolean,
    val recentAccuracy: Double
) {
    /** Effective difficulty band roughly 1..10 */
    fun level(): Int {
        val base = when {
            ageYears <= 6 -> 1
            ageYears <= 8 -> 3
            ageYears <= 10 -> 5
            ageYears <= 12 -> 7
            else -> 8
        }
        val accuracyAdjust = when {
            recentAccuracy < 0.4 -> -1
            recentAccuracy > 0.85 -> 1
            else -> 0
        }
        val easier = if (forceEasier) -2 else 0
        return (base + offset + accuracyAdjust + easier).coerceIn(1, 10)
    }
}
