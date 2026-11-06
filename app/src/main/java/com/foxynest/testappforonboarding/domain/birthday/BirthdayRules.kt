package com.foxynest.testappforonboarding.domain.birthday

import com.foxynest.testappforonboarding.domain.birthday.BirthdayRules.NOTIFY_TIME
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * «Мозг» напоминания о дне рождения: только расчёты с датами, без Android.
 *
 * Зачем отдельный объект без Android:
 * - всю логику «когда поздравлять» можно проверить обычными unit-тестами за доли секунды
 *   (`BirthdayRulesTest`), без эмулятора и без ожидания реального дня рождения;
 * - время «сейчас» приходит параметром, поэтому в тесте можно сказать «представь, что сейчас 14 сентября, 21:30».
 *
 * Android-часть (будильник, уведомление, хранение) – в `BirthdayNotificationRepository`, она только вызывает эти функции.
 * Подробный разбор всей схемы – в `docs/BIRTHDAY_NOTIFICATION.md`.
 *
 * Правила:
 * - 29 февраля в невисокосный год празднуем 28 февраля (так работает `LocalDate.withYear`).
 * - Поздравляем только в сам день рождения. Если устройство было выключено весь день – поздравления нет
 *   (запоздалое «вчера был день рождения» решили не показывать).
 * - В фоне (перезагрузка, смена времени, срабатывание будильника) не поздравляем раньше [NOTIFY_TIME],
 *   чтобы не беспокоить человека в 6 утра при включении телефона.
 * - Если пользователь сам в приложении (сохранил анкету, открыл приложение) – поздравляем сразу, в любое время дня:
 *   он и так держит телефон в руках.
 */
object BirthdayRules {
    /** Время утреннего поздравления. Чтобы поменять (например, на 10:00) – достаточно изменить здесь. */
    val NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)

    /**
     * Дата праздника в нужном году. `withYear` сам превращает 29.02 в 28.02, если год не високосный,
     * поэтому отдельных проверок «а високосный ли год» в коде нет.
     */
    fun birthdayInYear(birthDate: LocalDate, year: Int): LocalDate = birthDate.withYear(year)

    /**
     * Сегодня день рождения? Дата рождения в будущем (ошибка ввода) днём рождения не считается.
     * Год рождения роли не играет: 1950 и 2020 работают одинаково.
     */
    fun isBirthdayToday(birthDate: LocalDate, today: LocalDate): Boolean =
        !birthDate.isAfter(today) && birthdayInYear(birthDate, today.year) == today

    /**
     * Ключ «уже поздравили»: дата рождения + дата праздника, например `2006-09-15@2026-09-15`.
     * Сохраняется в DataStore после показа уведомления.
     *
     * Почему не просто «год последнего поздравления»:
     * - исправление опечатки в имени не приводит к повторному поздравлению (имени в ключе нет);
     * - смена ребёнка на другого (с другой датой рождения) – приводит, даже если праздник в тот же день.
     */
    fun occurrenceKey(birthDate: LocalDate, today: LocalDate): String = "$birthDate@$today"

    /**
     * Когда поставить будильник: ближайший момент строго после [now].
     * - сегодня в [NOTIFY_TIME], если сегодня день рождения и 9:00 ещё не наступило;
     * - иначе – день рождения в этом году (если ещё впереди) или в следующем, тоже в [NOTIFY_TIME].
     *
     * Пример из задачи: анкету сохранили 14 сентября в 21:30, день рождения 15-го →
     * будильник на 15 сентября 9:00 (а не «через 24 часа», как могло бы получиться при расчёте от текущего времени).
     */
    fun nextNotificationTime(birthDate: LocalDate, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate()
        val thisYear = birthdayInYear(birthDate, today.year)
        val date = when {
            thisYear.isAfter(today) -> thisYear
            thisYear == today && now.toLocalTime().isBefore(NOTIFY_TIME) -> thisYear
            // Берём год от исходной даты, а не thisYear.plusYears(1): иначе 29.02 после невисокосного года «залипнет» на 28.02
            else -> birthdayInYear(birthDate, today.year + 1)
        }
        return LocalDateTime.of(date, NOTIFY_TIME)
    }

    /**
     * Показать поздравление прямо сейчас?
     *
     * @param lastNotifiedKey ключ из [occurrenceKey], сохранённый после прошлого поздравления (null – ещё ни разу)
     * @param userIsInApp true – вызов из приложения (пользователь его видит), false – из фона (будильник, перезагрузка)
     *
     * Порядок проверок:
     * 1. сегодня не день рождения → нет;
     * 2. сегодня уже поздравляли → нет (уведомление не повторяется при каждом открытии приложения);
     * 3. пользователь в приложении → да, в любое время;
     * 4. фон → да, только если уже [NOTIFY_TIME] или позже. Так «телефон был выключен в 9:00, включили в 11:00»
     *    даёт поздравление сразу после включения, а «включили в 6:00» – ждёт будильника на 9:00.
     */
    fun shouldNotifyNow(
        birthDate: LocalDate,
        now: LocalDateTime,
        lastNotifiedKey: String?,
        userIsInApp: Boolean
    ): Boolean {
        val today = now.toLocalDate()
        if (!isBirthdayToday(birthDate, today)) return false
        if (lastNotifiedKey == occurrenceKey(birthDate, today)) return false
        return userIsInApp || !now.toLocalTime().isBefore(NOTIFY_TIME)
    }
}