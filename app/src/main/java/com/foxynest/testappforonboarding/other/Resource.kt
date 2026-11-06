package com.foxynest.testappforonboarding.other

/**
 * A general class, that can be reused in other projects
 *
 * We can check if the data was loaded correctly
 * So in fragment we just check if the Resource got success, loading or error status status
 * and according to it do what we need to do there
 */
// "out T" – Resource только ОТДАЁТ T наружу (data), но не принимает его в функции. Благодаря этому
// Resource<Int> можно передать туда, где ждут Resource<Number> или Resource<Any> («ковариантность»).
//
// В этом проекте Resource используется в двух ролях:
// - Resource<UserPresentation?> – данные + статус их загрузки;
// - Resource<ErrorType> – результат операции: SUCCESS (data = null) или ERROR (data = тип ошибки для текста на экране).
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    // functions for corresponding status from enum class Status
    companion object {
        fun <T> init(data: T?) = Resource(Status.INIT, data, null)

        fun <T> success(data: T?) = Resource(Status.SUCCESS, data, null)

        fun <T> error(message: String, data: T?) = Resource(Status.ERROR, data, message)

        // when you implement a cashing mechanism, you could already have a data that comes from the cash while you load the data that comes from the remote data source
        fun <T> loading(data: T?) = Resource(Status.LOADING, data, null)
    }
}

enum class Status {
    INIT,
    SUCCESS,
    ERROR,
    LOADING
}