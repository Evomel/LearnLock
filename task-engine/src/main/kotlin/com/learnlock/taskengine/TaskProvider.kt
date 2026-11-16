package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Контракт генератора задач одной [TaskCategory].
 *
 * Реализации должны детерминированно вычислять правильный ответ при фиксированных
 * [profile], [random] и входных данных шаблона.
 */
interface TaskProvider {
    /** Категория, для которой выдаёт задачи этот провайдер. */
    val category: TaskCategory

    /**
     * Создаёт одну задачу заданной сложности.
     *
     * @param profile профиль сложности (возраст, смещение, точность)
     * @param random источник случайности для вариативности формулировок и чисел
     * @param preferredSkill приоритетный навык той же категории; `null` — выбор по уровню
     * @return сгенерированная [Task]
     */
    fun generate(
        profile: DifficultyProfile,
        random: Random = Random.Default,
        preferredSkill: SkillTag? = null
    ): Task
}