package com.foxynest.testappforonboarding.presentation.common

import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.widget.EditText
import com.foxynest.testappforonboarding.domain.entities.PersonNameRules

/**
 * Фильтр поля ввода имени: молча не пропускает символы, которые не подходят для имени (цифры, эмодзи, «@» и т.п.).
 * Какие символы разрешены – решает domain ([PersonNameRules.isAllowedChar]), здесь только «подключение» к EditText.
 *
 * Как работает InputFilter: Android вызывает [filter] при каждом вводе или вставке текста.
 * - вернуть `null` – принять текст как есть;
 * - вернуть другую строку – вставить её вместо введённого.
 */
class PersonNameInputFilter : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        // Всё подходит – возвращаем null. Это важно: так клавиатура не теряет «набираемое» слово
        // (подчёркнутый текст с подсказками), иначе на некоторых клавиатурах буквы начинают дублироваться
        if ((start until end).all { PersonNameRules.isAllowedChar(source[it]) }) return null

        val filtered = StringBuilder()
        for (i in start until end) {
            if (PersonNameRules.isAllowedChar(source[i])) filtered.append(source[i])
        }

        // Если у исходного текста было оформление (span-ы), переносим его на отфильтрованный
        if (source is Spanned) {
            val spannable = SpannableString(filtered)
            TextUtils.copySpansFrom(
                source,
                start,
                filtered.length.coerceAtMost(end - start),
                null,
                spannable,
                0
            )
            return spannable
        }
        return filtered
    }
}

/** Подключает к полю правила имени: разрешённые символы + лимит длины из [PersonNameRules.MAX_LENGTH]. */
fun EditText.applyPersonNameRules() {
    // filters заменяет фильтры целиком, поэтому лимит длины добавляем здесь же, а не в XML (android:maxLength)
    filters = arrayOf(PersonNameInputFilter(), InputFilter.LengthFilter(PersonNameRules.MAX_LENGTH))
}