package com.learnlock.taskengine

/**
 * Запись шаблона задачи.
 * Формат файла: skillTag|level|template|hint
 */
data class TemplateEntry(
    val skillTag: SkillTag,
    val level: Int,
    val template: String,
    val hint: String
) {
    val variables: Set<String> = TemplateEngine.variablesOf(template)
}

object TemplateCatalog {
    private val mathCache = lazy { load("/vocabulary/math_templates.txt") }

    fun mathTemplates(): List<TemplateEntry> = mathCache.value

    fun mathFor(skill: SkillTag, level: Int): List<TemplateEntry> =
        mathTemplates().filter { it.skillTag == skill && it.level <= level.coerceIn(1, 10) }
            .ifEmpty { mathTemplates().filter { it.skillTag == skill } }

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
