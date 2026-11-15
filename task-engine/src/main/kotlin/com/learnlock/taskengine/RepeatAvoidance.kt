package com.learnlock.taskengine

import java.util.ArrayDeque

/**
 * Буфер недавно выданных задач по отпечатку содержания
 * (skill + нормализованный prompt), а не по случайному id.
 */
class RepeatAvoidance(private val capacity: Int = 40) {

    private val recent = ArrayDeque<String>(capacity)

    @Synchronized
    fun isRecent(key: String): Boolean = key in recent

    @Synchronized
    fun isRecent(task: Task): Boolean = isRecent(fingerprint(task))

    @Synchronized
    fun markSeen(key: String) {
        recent.remove(key)
        recent.addLast(key)
        while (recent.size > capacity) recent.removeFirst()
    }

    @Synchronized
    fun markSeen(task: Task) = markSeen(fingerprint(task))

    @Synchronized
    fun clear() = recent.clear()

    companion object {
        fun fingerprint(task: Task): String {
            val compact = Task.normalize(task.prompt)
                .replace(Regex("\\s+"), " ")
                .take(120)
            return "${task.skillTag}|$compact"
        }
    }
}
