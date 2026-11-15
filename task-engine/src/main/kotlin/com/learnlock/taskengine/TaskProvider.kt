package com.learnlock.taskengine

import kotlin.random.Random

interface TaskProvider {
    val category: TaskCategory
    fun generate(
        profile: DifficultyProfile,
        random: Random = Random.Default,
        preferredSkill: SkillTag? = null
    ): Task
}
