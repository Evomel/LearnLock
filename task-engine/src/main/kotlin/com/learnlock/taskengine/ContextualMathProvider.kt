package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Математика из шаблонов [TemplateCatalog] + словарь.
 * Ответ всегда вычисляется детерминированно.
 */
class ContextualMathProvider(
    private val repeatAvoidance: RepeatAvoidance = RepeatAvoidance(),
    private val generator: MathTemplateGenerator = MathTemplateGenerator()
) : TaskProvider {
    override val category: TaskCategory = TaskCategory.MATH

    override fun generate(
        profile: DifficultyProfile,
        random: Random,
        preferredSkill: SkillTag?
    ): Task {
        var task = generateOnce(profile, random, preferredSkill)
        var attempts = 0
        while (repeatAvoidance.isRecent(task) && attempts < 8) {
            task = generateOnce(profile, random, preferredSkill)
            attempts++
        }
        repeatAvoidance.markSeen(task)
        return task
    }

    private fun generateOnce(
        profile: DifficultyProfile,
        random: Random,
        preferredSkill: SkillTag?
    ): Task {
        val level = profile.level()
        val skill = preferredSkill?.takeIf { it.category == category } ?: skillByLevel(level, random)
        val pool = TemplateCatalog.mathFor(skill, level)
        if (pool.isEmpty()) {
            return fallbackAdd(level, random)
        }
        val entry = pool.random(random)
        return generator.generate(entry, profile, random)
    }

    private fun skillByLevel(level: Int, random: Random): SkillTag = when {
        level <= 2 -> SkillTag.MATH_ADD
        level <= 4 -> if (random.nextBoolean()) SkillTag.MATH_SUB else SkillTag.MATH_ADD
        level <= 6 -> listOf(SkillTag.MATH_MUL, SkillTag.MATH_COMPARE, SkillTag.MATH_SUB).random(random)
        else -> SkillTag.MATH_MIX
    }

    private fun fallbackAdd(level: Int, random: Random): Task {
        val a = random.nextInt(1, 10)
        val b = random.nextInt(1, 10)
        return Task(
            id = "math-add-$a-$b",
            category = category,
            prompt = "Сколько будет $a + $b?",
            inputType = AnswerInputType.NUMBER,
            acceptedAnswers = listOf((a + b).toString()),
            hint = "Сложи числа",
            difficultyLevel = level,
            skillTag = SkillTag.MATH_ADD
        )
    }
}
