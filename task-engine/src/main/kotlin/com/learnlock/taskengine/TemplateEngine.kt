package com.learnlock.taskengine

/**
 * Подстановка переменных в текстовый шаблон вида `У {name} {count} {item}`.
 */
object TemplateEngine {
    // На Android (ICU) закрывающая } тоже должна быть экранирована.
    private val pattern = Regex("\\{([a-zA-Z0-9_]+)\\}")

    /**
     * Заменяет плейсхолдеры `{ключ}` значениями из карты; неизвестные ключи оставляет как есть.
     *
     * @param template строка шаблона
     * @param variables соответствие имён переменных и подставляемых значений
     * @return отрендеренный текст
     */
    fun render(template: String, variables: Map<String, String>): String =
        template.replace(pattern) { match ->
            variables[match.groupValues[1]] ?: match.value
        }

    /**
     * Извлекает множество имён переменных из шаблона.
     *
     * @param template строка с плейсхолдерами
     * @return уникальные идентификаторы в фигурных скобках
     */
    fun variablesOf(template: String): Set<String> =
        pattern.findAll(template).map { it.groupValues[1] }.toSet()
}