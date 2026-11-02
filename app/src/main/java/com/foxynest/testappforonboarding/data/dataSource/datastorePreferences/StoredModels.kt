package com.foxynest.testappforonboarding.data.dataSource.datastorePreferences

import androidx.annotation.Keep
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.google.gson.annotations.SerializedName

/**
 * Формат хранения в DataStore (JSON через Gson). Отделён от domain-моделей:
 *
 * 1. @SerializedName фиксирует ключи JSON. При включении R8 (minifyEnabled) поля классов переименовываются
 *    в a, b, c…, и без аннотаций новая версия приложения не прочитала бы уже сохранённые у пользователей данные.
 * 2. Значения ключей совпадают с прежними именами полей – уже сохранённые данные читаются как раньше.
 * 3. Переименование поля в domain больше не ломает хранилище: меняется только маппинг ниже.
 *
 * Значения по умолчанию у всех параметров обязательны: тогда у класса есть конструктор без аргументов,
 * и Gson не оставит null в не-null полях, если в старом JSON какого-то ключа нет.
 */
@Keep
internal data class OnboardingAnswersDto(
    @SerializedName("userName") val userName: String? = "",
    @SerializedName("childGender") val childGender: String? = "",
    @SerializedName("isItABoy") val isItABoy: String? = "",
    @SerializedName("childName") val childName: String? = "",
    @SerializedName("childBirthDate") val childBirthDate: String? = "",
    @SerializedName("childHobbies") val childHobbies: String? = ""
)

@Keep
internal data class BirthdayNotificationDto(
    @SerializedName("childName") val childName: String? = "",
    @SerializedName("childBirthDate") val childBirthDate: String? = ""
)

internal fun OnboardingAnswersDto.toDomain() = OnboardingAnswers(
    userName = userName.orEmpty(),
    childGender = childGender.orEmpty(),
    isItABoy = isItABoy.orEmpty(),
    childName = childName.orEmpty(),
    childBirthDate = childBirthDate.orEmpty(),
    childHobbies = childHobbies.orEmpty()
)

internal fun OnboardingAnswers.toDto() = OnboardingAnswersDto(
    userName = userName,
    childGender = childGender,
    isItABoy = isItABoy,
    childName = childName,
    childBirthDate = childBirthDate,
    childHobbies = childHobbies
)

internal fun BirthdayNotificationDto.toDomain() = BirthdayNotificationData(
    childName = childName.orEmpty(),
    childBirthDate = childBirthDate.orEmpty()
)

internal fun BirthdayNotificationData.toDto() = BirthdayNotificationDto(
    childName = childName,
    childBirthDate = childBirthDate
)