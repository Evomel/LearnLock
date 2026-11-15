package com.learnlock.taskengine

import kotlin.random.Random

/** Небольшие безопасные вариации формулировок для типовых заданий. */
class TextVariator(private val random: Random) {

    fun pick(variants: List<String>): String = variants.random(random)

    fun askSum(): String = pick(listOf(
        "Сколько будет", "Чему равно", "Найди сумму", "Вычисли", "Посчитай"
    ))

    fun askDiff(): String = pick(listOf(
        "Сколько будет", "Найди разность", "Вычисли", "Посчитай"
    ))

    fun askProduct(): String = pick(listOf(
        "Сколько будет", "Найди произведение", "Вычисли", "Посчитай"
    ))

    fun askCompare(): String = pick(listOf(
        "Что больше", "Сравни числа", "Выбери верное неравенство"
    ))

    fun askOdd(): String = pick(listOf(
        "Какое слово лишнее", "Найди лишнее слово", "Какое слово не подходит"
    ))

    fun askNext(): String = pick(listOf(
        "Что дальше", "Какое число пропущено", "Продолжи последовательность"
    ))

    fun askPattern(): String = pick(listOf(
        "Какой знак дальше", "Что идёт дальше", "Какой элемент пропущен"
    ))

    fun intro(): String = pick(listOf(
        "Задача:", "Внимание!", "Реши:", "Подумай:"
    ))
}
