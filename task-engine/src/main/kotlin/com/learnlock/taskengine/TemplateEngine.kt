package com.learnlock.taskengine

/** Рендерит шаблон вида "У {name} {count} {item}" по карте переменных. */
object TemplateEngine {
    private val pattern = Regex("\\{([a-zA-Z0-9_]+)}")

    fun render(template: String, variables: Map<String, String>): String =
        template.replace(pattern) { match ->
            variables[match.groupValues[1]] ?: match.value
        }

    fun variablesOf(template: String): Set<String> =
        pattern.findAll(template).map { it.groupValues[1] }.toSet()
}
