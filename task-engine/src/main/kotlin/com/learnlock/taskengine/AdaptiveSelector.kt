package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Refines a [TaskRequest] using local learner stats: boost weak skills,
 * slightly dampen strong ones, optionally pin a preferred weak skill.
 */
class AdaptiveSelector(
    private val preferWeakChance: Double = 0.4,
    private val random: Random = Random.Default
) {
    data class RefinedRequest(
        val request: TaskRequest,
        val categoryWeights: Map<TaskCategory, Int>,
        val preferredSkill: SkillTag?,
        val preferredCategory: TaskCategory?
    )

    fun refine(request: TaskRequest): RefinedRequest {
        val stats = request.learnerStats
        if (stats == null) {
            return RefinedRequest(
                request = request,
                categoryWeights = request.categoryWeights,
                preferredSkill = request.preferredSkill,
                preferredCategory = request.preferredCategory
            )
        }

        val enabled = request.enabledCategories
        val weak = stats.weakSkills().filter { it.category in enabled }
        val strong = stats.strongSkills().filter { it.category in enabled }

        val weights = request.categoryWeights.toMutableMap()
        enabled.forEach { cat ->
            val base = (weights[cat] ?: 1).coerceAtLeast(1)
            val catAcc = stats.categoryAccuracy[cat]
            var factor = 1.0
            if (catAcc != null) {
                if (catAcc < 0.55) factor += 0.8
                if (catAcc > 0.85) factor -= 0.25
            }
            if (weak.any { it.category == cat }) factor += 0.5
            if (strong.any { it.category == cat } && weak.none { it.category == cat }) {
                factor -= 0.2
            }
            weights[cat] = (base * factor).toInt().coerceIn(1, 10)
        }

        var preferredSkill = request.preferredSkill
        var preferredCategory = request.preferredCategory
        if (preferredSkill == null && weak.isNotEmpty() && random.nextDouble() < preferWeakChance) {
            preferredSkill = weak.first()
            preferredCategory = preferredSkill.category
        }

        var offset = request.difficultyOffset
        var forceEasier = request.forceEasier
        val accuracy = stats.overallAccuracy.takeIf { it > 0 } ?: request.recentAccuracy

        if (preferredSkill != null && preferredSkill in weak) {
            forceEasier = true
            offset = (offset - 1).coerceIn(-2, 2)
        } else if (stats.overallAccuracy > 0.85 && weak.isEmpty()) {
            offset = (offset + 1).coerceIn(-2, 2)
        }

        val refined = request.copy(
            categoryWeights = weights,
            preferredSkill = preferredSkill,
            preferredCategory = preferredCategory,
            difficultyOffset = offset,
            forceEasier = forceEasier,
            recentAccuracy = accuracy
        )
        return RefinedRequest(
            request = refined,
            categoryWeights = weights,
            preferredSkill = preferredSkill,
            preferredCategory = preferredCategory
        )
    }
}
