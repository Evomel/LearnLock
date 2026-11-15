package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Словарные слова начальной школы (ориентир: УМК «Школа России», 1–4 класс).
 * Источник: публичные списки словарных слов; единого ФГОС-списка нет.
 */
object SchoolVocabulary {
    data class Entry(val grade: Int, val word: String)

    data class WordItem(val word: String, val missingIndex: Int)

    private val vowels = setOf('А', 'Е', 'Ё', 'И', 'О', 'У', 'Ы', 'Э', 'Ю', 'Я')

    val entries: List<Entry> by lazy { loadEntries() }

    /** Уникальные слова до указанного класса включительно. */
    fun wordsUpToGrade(grade: Int): List<String> =
        entries.filter { it.grade <= grade.coerceIn(1, 4) }
            .map { it.word }
            .distinctBy { it.lowercase() }

    fun gradeForAge(ageYears: Int): Int = when {
        ageYears <= 7 -> 1
        ageYears <= 8 -> 2
        ageYears <= 9 -> 3
        else -> 4
    }

    fun wordItemsForAge(ageYears: Int): List<WordItem> {
        val grade = gradeForAge(ageYears)
        return wordsUpToGrade(grade)
            .mapNotNull { toWordItem(it) }
    }

    fun toWordItem(raw: String): WordItem? {
        // Для пропуска буквы берём только однословные формы без пробелов/дефисов
        val cleaned = raw.trim()
            .replace('ё', 'е')
            .replace('Ё', 'Е')
        if (cleaned.contains(' ') || cleaned.contains('-')) return null
        val upper = cleaned.uppercase().filter { it in 'А'..'Я' }
        if (upper.length < 3) return null
        val index = pickMissingIndex(upper) ?: return null
        return WordItem(upper, index)
    }

    fun pickMissingIndex(upperWord: String, random: Random = Random.Default): Int? {
        if (upperWord.length < 3) return null
        val vowelIdx = upperWord.indices.filter { i ->
            upperWord[i] in vowels && i > 0 && i < upperWord.lastIndex
        }
        if (vowelIdx.isNotEmpty()) return vowelIdx.random(random)
        val mid = upperWord.indices.filter { it > 0 }
        return mid.randomOrNull(random)
    }

    private fun loadEntries(): List<Entry> {
        val stream = SchoolVocabulary::class.java.getResourceAsStream("/vocabulary/school_russia_1_4.txt")
            ?: error("Missing resource /vocabulary/school_russia_1_4.txt")
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('|', limit = 2)
                    if (parts.size != 2) return@mapNotNull null
                    val grade = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val word = parts[1].trim()
                    if (word.isEmpty()) null else Entry(grade, word)
                }
                .toList()
        }
    }
}
