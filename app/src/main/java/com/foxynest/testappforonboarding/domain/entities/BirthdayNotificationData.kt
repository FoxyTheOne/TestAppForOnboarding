package com.foxynest.testappforonboarding.domain.entities

/**
 * Всё, что нужно для поздравления: имя (для текста уведомления) и дата рождения в формате `yyyy-MM-dd`.
 *
 * Хранится отдельно от анкеты (своя запись в DataStore), потому что будильник срабатывает в фоне:
 * ему не нужно ни загружать профиль из Firebase, ни знать, вошёл ли пользователь в аккаунт.
 */
data class BirthdayNotificationData(
    val childName: String = "",
    val childBirthDate: String = ""
)

/** Есть и имя, и дата. Без них планировать нечего. */
fun BirthdayNotificationData.isValidData(): Boolean =
    childName.isNotBlankValue() && childBirthDate.isNotBlankValue()