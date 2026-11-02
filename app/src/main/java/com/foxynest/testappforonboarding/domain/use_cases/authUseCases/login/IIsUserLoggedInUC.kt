package com.foxynest.testappforonboarding.domain.use_cases.authUseCases.login

interface IIsUserLoggedInUC {
    /** Без сетевых запросов – только локальное состояние авторизации. */
    fun execute(): Boolean
}