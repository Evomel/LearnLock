package com.learnlock.taskengine

/**
 * Запись шаблона математической (или иной) задачи из ресурсного файла.
 *
 * Формат строки ресурса: `skillTag|level|template|hint`
 *
 * @property level максимальный уровень сложности, для которого применим шаблон (1..10)
 * @property template текст с плейсхолдерами `{var}`
 */
data class TemplateEntry(
    val skillTag: SkillTag,
    val level: Int,
    val template: String,
    val hint: String
) {
    /** Имена переменных, извлечённые из [template]. */
    val variables: Set<String> = TemplateEngine.variablesOf(template)
}

/**
 * Ленивая загрузка каталога шаблонов из classpath (`/vocabulary/math_templates.txt`).
 */
object TemplateCatalog {
    private val mathCache = lazy { load("/vocabulary/math_templates.txt") }

    /** Все загруженные математические шаблоны. */
    fun mathTemplates(): List<TemplateEntry> = mathCache.value

    /**
     * Шаблоны для навыка, подходящие для данного [level] (фильтр `entry.level <= level`).
     *
     * Если подходящих нет — возвращает все шаблоны данного навыка без фильтра по уровню.
     *
     * @param skill тег математического навыка
     * @param level текущий уровень сложности ученика
     */
    fun mathFor(skill: SkillTag, level: Int): List<TemplateEntry> =
        mathTemplates().filter { it.skillTag == skill && it.level <= level.coerceIn(1, 10) }
            .ifEmpty { mathTemplates().filter { it.skillTag == skill } }

    /** Читает и парсит файл шаблонов; отсутствующий ресурс даёт пустой список. */
    private fun load(path: String): List<TemplateEntry> {
        val stream = TemplateCatalog::class.java.getResourceAsStream(path)
            ?: return emptyList()
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { parse(it) }
                .toList()
        }
    }

    /** Разбирает одну строку каталога; некорректные строки отбрасываются. */
    private fun parse(line: String): TemplateEntry? {
        val parts = line.split('|')
        if (parts.size < 4) return null
        val skill = runCatching { SkillTag.valueOf(parts[0].trim()) }.getOrNull() ?: return null
        val level = parts[1].trim().toIntOrNull()?.coerceIn(1, 10) ?: return null
        val template = parts[2].trim()
        val hint = parts[3].trim()
        if (template.isEmpty()) return null
        return TemplateEntry(skill, level, template, hint)
    }
}