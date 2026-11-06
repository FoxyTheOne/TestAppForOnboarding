package com.foxynest.testappforonboarding.other

/**
 * A general class, that can be reused in other projects
 *
 * Triggers a specific event
 * Once we trigger an event, we will set var hasBeenHandled to true and then it just won't emmit this event again and will just emmit null
 * It will be useful for error messages, for instance
 *
 * Зачем: LiveData хранит последнее значение и отдаёт его каждому новому наблюдателю. Тост «ошибка»
 * или переход на экран, положенные в LiveData как есть, повторились бы при повороте экрана или
 * возврате на него. Обёртка MyEvent отдаёт содержимое только один раз ([getContentIfNotHandled]).
 *
 * Правило проекта: **событие** («покажи тост», «перейди») – MyEvent; **состояние** («идёт загрузка»,
 * «ошибка в поле») – обычное значение, его как раз нужно показать снова после пересоздания экрана.
 */
open class MyEvent<out T>(private val data: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    // if you need to pick up data, even if it has already been handled
    fun peekContent() = data
}