package com.learnlock.taskengine

import kotlin.random.Random

/**
 * Сборка математической [Task] из [TemplateEntry]: случайные операнды,
 * подстановка в шаблон и расчёт принимаемого ответа по типу навыка.
 */
class MathTemplateGenerator {
    private val namesNom = listOf(
        "Миша", "Петя", "Аня", "Игорь", "Серёжа", "Оля", "Катя", "Дима", "Лена", "Витя", "Саша", "Настя"
    )
    private val namesGen = listOf(
        "Миши", "Пети", "Ани", "Игоря", "Серёжи", "Оли", "Кати", "Димы", "Лены", "Вити", "Саши", "Насти"
    )
    private val items = listOf(
        "яблок", "груш", "конфет", "карандашей", "наклеек", "шаров", "машинок", "книг", "мячей", "печений"
    )

    /**
     * Генерирует задачу по записи каталога и профилю сложности.
     *
     * Диапазоны `a`, `b`, `c` и контекстные переменные подбираются по [DifficultyProfile.level]
     * и [TemplateEntry.skillTag].
     */
    fun generate(entry: TemplateEntry, profile: DifficultyProfile, random: Random): Task {
        val level = profile.level()
        val bound = when {
            level <= 2 -> 6
            level <= 4 -> 12
            level <= 6 -> 20
            else -> 40
        }

        var a = random.nextInt(1, bound + 1)
        var b = random.nextInt(1, bound + 1)
        var c = random.nextInt(1, (bound / 2).coerceAtLeast(2) + 1)

        when (entry.skillTag) {
            SkillTag.MATH_SUB, SkillTag.MATH_MIX -> {
                if (b >= a) {
                    val t = a
                    a = b + random.nextInt(1, 5)
                    b = t
                }
            }
            SkillTag.MATH_MUL -> {
                a = random.nextInt(2, if (level <= 5) 9 else 12)
                b = random.nextInt(2, if (level <= 5) 9 else 12)
            }
            else -> Unit
        }

        val price = random.nextInt(2, 20) * 50
        val money = random.nextInt(1, 25) * 50
        val nameIdx = random.nextInt(namesGen.size)
        val nameIdx2 = (nameIdx + 1 + random.nextInt(namesGen.size - 1)) % namesGen.size
        val item = items.random(random)
        val compareOp = when {
            a > b -> ">"
            a < b -> "<"
            else -> "="
        }

        val vars = mutableMapOf(
            "a" to a.toString(),
            "b" to b.toString(),
            "c" to c.toString(),
            "sum" to (a + b).toString(),
            "diff" to (a - b).toString(),
            "prod" to (a * b).toString(),
            "a_name" to namesGen[nameIdx],
            "b_name" to namesGen[nameIdx2],
            "item" to item,
            "price" to price.toString(),
            "money" to money.toString(),
            "op" to "?",
            "count" to a.toString(),
            "name" to namesGen[nameIdx]
        )
        entry.variables.forEach { key ->
            if (key !in vars) vars[key] = a.toString()
        }

        val prompt = TemplateEngine.render(entry.template, vars)
        return buildTask(
            entry = entry,
            prompt = prompt,
            a = a,
            b = b,
            c = c,
            price = price,
            money = money,
            nameA = namesNom[nameIdx],
            nameB = namesNom[nameIdx2],
            level = level
        )
    }

    /** Маршрутизация к типу задачи (число, сравнение, смешанная) по [entry.skillTag]. */
    private fun buildTask(
        entry: TemplateEntry,
        prompt: String,
        a: Int,
        b: Int,
        c: Int,
        price: Int,
        money: Int,
        nameA: String,
        nameB: String,
        level: Int
    ): Task {
        val lower = prompt.lowercase()
        return when (entry.skillTag) {
            SkillTag.MATH_ADD -> numberTask(entry, prompt, a + b, level)
            SkillTag.MATH_SUB -> numberTask(entry, prompt, a - b, level)
            SkillTag.MATH_MUL -> numberTask(entry, prompt, a * b, level)
            SkillTag.MATH_COMPARE -> compareTask(entry, prompt, lower, a, b, nameA, nameB, level)
            SkillTag.MATH_MIX -> mixTask(entry, prompt, lower, a, b, c, price, money, level)
            else -> numberTask(entry, prompt, a + b, level)
        }
    }

    /**
     * Задание на сравнение: кто больше, какое число больше или знак неравенства.
     * Ветвление по ключевым фразам в [lower].
     */
    private fun compareTask(
        entry: TemplateEntry,
        prompt: String,
        lower: String,
        a: Int,
        b: Int,
        nameA: String,
        nameB: String,
        level: Int
    ): Task {
        if ("у кого" in lower) {
            val winner = when {
                a > b -> nameA
                a < b -> nameB
                else -> nameA
            }
            val options = listOf(nameA, nameB).distinct()
            return Task(
                id = "math-cmp-who-$a-$b",
                category = TaskCategory.MATH,
                prompt = prompt,
                inputType = AnswerInputType.CHOICE,
                choices = options.mapIndexed { i, l -> TaskChoice(i.toString(), l) },
                acceptedAnswers = listOf(winner),
                hint = entry.hint,
                difficultyLevel = level,
                skillTag = SkillTag.MATH_COMPARE
            )
        }
        if ("что больше" in lower || "или" in lower) {
            val larger = maxOf(a, b).toString()
            val options = listOf(a, b).distinct().map { it.toString() }
            return Task(
                id = "math-cmp-num-$a-$b",
                category = TaskCategory.MATH,
                prompt = prompt,
                inputType = AnswerInputType.CHOICE,
                choices = options.mapIndexed { i, l -> TaskChoice(i.toString(), l) },
                acceptedAnswers = listOf(larger),
                hint = entry.hint,
                difficultyLevel = level,
                skillTag = SkillTag.MATH_COMPARE
            )
        }
        val op = when {
            a > b -> ">"
            a < b -> "<"
            else -> "="
        }
        return Task(
            id = "math-cmp-$a-$b",
            category = TaskCategory.MATH,
            prompt = prompt,
            inputType = AnswerInputType.CHOICE,
            choices = listOf(
                TaskChoice("gt", ">"),
                TaskChoice("lt", "<"),
                TaskChoice("eq", "=")
            ),
            acceptedAnswers = listOf(op),
            hint = entry.hint,
            difficultyLevel = level,
            skillTag = SkillTag.MATH_COMPARE
        )
    }

    /**
     * Смешанные контекстные задачи: «хватит ли денег» или числовой ответ по эвристикам текста.
     */
    private fun mixTask(
        entry: TemplateEntry,
        prompt: String,
        lower: String,
        a: Int,
        b: Int,
        c: Int,
        price: Int,
        money: Int,
        level: Int
    ): Task {
        if ("хватит" in lower) {
            val answer = if (money >= price) "да" else "нет"
            return Task(
                id = "math-money-$price-$money",
                category = TaskCategory.MATH,
                prompt = prompt,
                inputType = AnswerInputType.CHOICE,
                choices = listOf(TaskChoice("y", "Да"), TaskChoice("n", "Нет")),
                acceptedAnswers = listOf(answer),
                hint = entry.hint,
                difficultyLevel = level,
                skillTag = SkillTag.MATH_MIX
            )
        }
        val result = when {
            "×" in prompt || "*" in prompt -> a * b + c
            "вышли" in lower -> a - b + c
            "прилетело" in lower || "улетело" in lower -> a + b - c
            "купил" in lower || "отдал" in lower -> a + b - c
            else -> a + b - c
        }
        return numberTask(entry, prompt, result, level)
    }

    /** Задача с числовым вводом и единственным целочисленным ответом. */
    private fun numberTask(entry: TemplateEntry, prompt: String, answer: Int, level: Int): Task =
        Task(
            id = "math-${entry.skillTag.name.lowercase()}-$answer-${prompt.hashCode()}",
            category = TaskCategory.MATH,
            prompt = prompt,
            inputType = AnswerInputType.NUMBER,
            acceptedAnswers = listOf(answer.toString()),
            hint = entry.hint,
            difficultyLevel = level,
            skillTag = entry.skillTag
        )
}