package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Платформо-независимый оркестратор генерации задач.
 *
 * Выбирает провайдер по весам и адаптивным приоритетам, строит [DifficultyProfile]
 * и делегирует генерацию. Подходит для Android и возможного KMP/iOS.
 *
 * @param providers список провайдеров по категориям
 * @param random общий источник случайности
 * @param adaptiveSelector модуль адаптации запроса по статистике
 */
class TaskEngine(
    private val providers: List<TaskProvider> = listOf(
        ContextualMathProvider(),
        VocabularyReadingProvider(),
        ExpandedLogicProvider()
    ),
    private val random: Random = Random.Default,
    private val adaptiveSelector: AdaptiveSelector = AdaptiveSelector(random = random)
) {
    /**
     * Возвращает следующую задачу по запросу ученика.
     *
     * @param request параметры возраста, категорий, весов и статистики
     * @return одна [Task] от выбранного провайдера
     * @throws IllegalArgumentException если не включена ни одна категория
     */
    fun nextTask(request: TaskRequest): Task {
        val enabled = providers.filter { it.category in request.enabledCategories }
        require(enabled.isNotEmpty()) { "At least one task category must be enabled" }

        val refined = adaptiveSelector.refine(request)
        val req = refined.request

        val preferredSkill = refined.preferredSkill
            ?.takeIf { it.category in request.enabledCategories }

        val preferredProvider = preferredSkill
            ?.let { skill -> enabled.firstOrNull { it.category == skill.category } }
            ?: refined.preferredCategory
                ?.let { category -> enabled.firstOrNull { it.category == category } }

        val provider = preferredProvider ?: pickWeighted(enabled, refined.categoryWeights)
        val profile = DifficultyProfile(
            ageYears = req.ageYears.coerceIn(5, 16),
            offset = req.difficultyOffset.coerceIn(-2, 2),
            forceEasier = req.forceEasier,
            recentAccuracy = req.recentAccuracy.coerceIn(0.0, 1.0)
        )
        return provider.generate(profile, random, preferredSkill)
    }

    /**
     * Случайный выбор провайдера с учётом целочисленных весов категорий
     * (каждый провайдер попадает в «корзину» столько раз, каков его вес).
     */
    private fun pickWeighted(
        enabled: List<TaskProvider>,
        weights: Map<TaskCategory, Int>
    ): TaskProvider {
        val weighted = enabled.flatMap { provider ->
            val weight = (weights[provider.category] ?: 1).coerceAtLeast(0)
            List(weight.coerceAtLeast(1)) { provider }
        }
        return weighted.random(random)
    }
}