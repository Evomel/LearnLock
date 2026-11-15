package com.learnlock.taskengine

/**
 * Загружает словарь из ресурсов vocabulary/words_{level}.txt.
 * Формат строки: category|word1|word2|...
 * Загружается лениво и кэшируется — память < 1 МБ.
 */
class VocabularyDatabase {

    enum class Level { EASY, MEDIUM, HARD }

    private val cache = mutableMapOf<Level, Map<String, List<String>>>()

    fun words(level: Level, category: String): List<String> =
        data(level)[category].orEmpty()

    fun categories(level: Level): Set<String> = data(level).keys

    fun randomWord(level: Level, category: String, random: kotlin.random.Random): String? =
        words(level, category).randomOrNull(random)

    fun pickDistinct(level: Level, category: String, count: Int, random: kotlin.random.Random): List<String> =
        words(level, category).shuffled(random).take(count)

    private fun data(level: Level): Map<String, List<String>> =
        cache.getOrPut(level) { load(level) }

    private fun load(level: Level): Map<String, List<String>> {
        val path = "/vocabulary/words_${level.name.lowercase()}.txt"
        val stream = VocabularyDatabase::class.java.getResourceAsStream(path)
            ?: return emptyMap()
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('|')
                    if (parts.size < 2) return@mapNotNull null
                    parts[0].trim() to parts.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
                }
                .toMap()
        }
    }
}
