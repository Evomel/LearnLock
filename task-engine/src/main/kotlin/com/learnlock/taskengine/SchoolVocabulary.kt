package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Словарные слова начальной школы (ориентир: УМК «Школа России», 1–4 класс).
 *
 * Источник: публичные списки словарных слов; единого ФГОС-списка нет.
 */
object SchoolVocabulary {
    /**
     * Запись словаря: класс и слово в канонической форме из ресурса.
     *
     * @property grade номер класса (1..4)
     */
    data class Entry(val grade: Int, val word: String)

    /**
     * Слово и индекс пропускаемой буквы для задания на чтение.
     *
     * @property word слово в верхнем регистре без ё
     * @property missingIndex позиция пропуска в [word]
     */
    data class WordItem(val word: String, val missingIndex: Int)

    private val vowels = setOf('А', 'Е', 'Ё', 'И', 'О', 'У', 'Ы', 'Э', 'Ю', 'Я')

    /** Полный список записей из `/vocabulary/school_russia_1_4.txt` (ленивая инициализация). */
    val entries: List<Entry> by lazy { loadEntries() }

    /**
     * Уникальные слова до указанного класса включительно (без учёта регистра дубликатов).
     *
     * @param grade верхняя граница класса; значение ограничивается 1..4
     */
    fun wordsUpToGrade(grade: Int): List<String> =
        entries.filter { it.grade <= grade.coerceIn(1, 4) }
            .map { it.word }
            .distinctBy { it.lowercase() }

    /**
     * Сопоставление возраста ученика классу словаря (1..4).
     */
    fun gradeForAge(ageYears: Int): Int = when {
        ageYears <= 7 -> 1
        ageYears <= 8 -> 2
        ageYears <= 9 -> 3
        else -> 4
    }

    /**
     * Слова с валидным пропуском буквы для заданий «вставь букву» по возрасту.
     */
    fun wordItemsForAge(ageYears: Int): List<WordItem> {
        val grade = gradeForAge(ageYears)
        return wordsUpToGrade(grade)
            .mapNotNull { toWordItem(it) }
    }

    /**
     * Преобразует строку словаря в [WordItem] или `null`, если форма не подходит для пропуска.
     *
     * Для пропуска буквы берутся только однословные формы без пробелов и дефисов.
     */
    fun toWordItem(raw: String): WordItem? {
        val cleaned = raw.trim()
            .replace('ё', 'е')
            .replace('Ё', 'Е')
        if (cleaned.contains(' ') || cleaned.contains('-')) return null
        val upper = cleaned.uppercase().filter { it in 'А'..'Я' }
        if (upper.length < 3) return null
        val index = pickMissingIndex(upper) ?: return null
        return WordItem(upper, index)
    }

    /**
     * Индекс буквы для пропуска: предпочтительно гласная не на краях слова.
     *
     * @param upperWord слово в верхнем регистре
     * @return индекс или `null`, если слово слишком короткое
     */
    fun pickMissingIndex(upperWord: String, random: Random = Random.Default): Int? {
        if (upperWord.length < 3) return null
        val vowelIdx = upperWord.indices.filter { i ->
            upperWord[i] in vowels && i > 0 && i < upperWord.lastIndex
        }
        if (vowelIdx.isNotEmpty()) return vowelIdx.random(random)
        val mid = upperWord.indices.filter { it > 0 }
        return mid.randomOrNull(random)
    }

    /** Загрузка и разбор ресурса формата `grade|слово`. */
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