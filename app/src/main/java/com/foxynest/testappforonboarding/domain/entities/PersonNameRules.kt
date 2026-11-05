package com.foxynest.testappforonboarding.domain.entities

/**
 * Правила для имени человека (имя пользователя и имя ребёнка). Чистый Kotlin, без Android,
 * поэтому одни и те же правила используются при вводе, при проверке перед сохранением и в unit-тестах.
 *
 * Разрешено:
 * - буквы **любого** алфавита (кириллица, латиница с диакритикой – «Zoë», «Ľubica», казахские, грузинские и т.д.);
 * - пробел – «Анна Мария», «Mary Ann»;
 * - дефис – «Анна-Мария», «Жан-Поль»;
 * - апостроф – «Д'Артаньян», «O'Brien» (обычный ' и типографский ’).
 *
 * Почему не `android:digits` в разметке: он задаёт список символов вручную (все алфавиты не перечислишь),
 * подменяет тип клавиатуры и на части клавиатур ломает подсказки и автоисправление.
 */
object PersonNameRules {

    /**
     * Максимальная длина. ДОЛЖНА СОВПАДАТЬ с лимитом в firestore.rules
     * (`username.size() <= 100`, `childName.size() <= 100`), иначе сервер отклонит сохранение профиля.
     */
    const val MAX_LENGTH = 100

    private val SEPARATORS = setOf(' ', '-', '\'', '’')

    /**
     * Можно ли ввести этот символ. Используется фильтром поля ввода.
     * NON_SPACING_MARK – «надстрочные» знаки: некоторые клавиатуры вводят «й» как «и» + знак краткости.
     */
    fun isAllowedChar(char: Char): Boolean =
        char.isLetter() || char.category == CharCategory.NON_SPACING_MARK || char in SEPARATORS

    /** Убирает пробелы по краям и схлопывает несколько пробелов подряд в один: «  Анна   Мария » → «Анна Мария». */
    fun normalize(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    /**
     * Проверка уже нормализованного имени.
     * @return null – имя подходит, иначе тип ошибки для показа под полем.
     */
    fun validate(name: String): ErrorType? = when {
        name.isBlank() -> ErrorType.FIELD_EMPTY_ERROR
        name.length > MAX_LENGTH -> ErrorType.NAME_INVALID_ERROR
        // Например, имя вставили из буфера обмена в обход фильтра ввода
        name.any { !isAllowedChar(it) } -> ErrorType.NAME_INVALID_ERROR
        // Хотя бы одна буква: «- - -» или «'» именем не считаем
        name.none { it.isLetter() } -> ErrorType.NAME_INVALID_ERROR
        // Разделитель только между буквами: не «-Анна», не «Анна-», не «Анна--Мария»
        name.first() in SEPARATORS || name.last() in SEPARATORS -> ErrorType.NAME_INVALID_ERROR
        name.zipWithNext()
            .any { (a, b) -> a in SEPARATORS && b in SEPARATORS } -> ErrorType.NAME_INVALID_ERROR

        else -> null
    }
}