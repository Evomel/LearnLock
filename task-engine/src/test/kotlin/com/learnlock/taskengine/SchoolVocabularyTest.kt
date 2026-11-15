package com.learnlock.taskengine

import kotlin.test.Test
import kotlin.test.assertTrue

class SchoolVocabularyTest {
    @Test
    fun loadsAtLeastTwoHundredUniqueWords() {
        val unique = SchoolVocabulary.entries.map { it.word.lowercase() }.distinct()
        assertTrue(unique.size >= 200, "expected >=200 unique words, got ${unique.size}")
    }

    @Test
    fun buildsWordItemsForLetterTasks() {
        val items = SchoolVocabulary.wordItemsForAge(10)
        assertTrue(items.size >= 150, "expected many letter-gap items, got ${items.size}")
        assertTrue(items.all { it.word.length >= 3 })
        assertTrue(items.all { it.missingIndex in it.word.indices })
    }

    @Test
    fun youngerAgeGetsSmallerPool() {
        val g1 = SchoolVocabulary.wordItemsForAge(7).size
        val g4 = SchoolVocabulary.wordItemsForAge(11).size
        assertTrue(g4 >= g1)
    }
}
