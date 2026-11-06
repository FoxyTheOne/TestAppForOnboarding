package com.foxynest.testappforonboarding.data

import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.BirthdayNotificationDto
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.OnboardingAnswersDto
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.toDomain
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.toDto
import com.foxynest.testappforonboarding.data.dataSource.firebase.UserFirestoreMapper
import com.foxynest.testappforonboarding.domain.entities.onboarding.OnboardingAnswers
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Защита от «обновил приложение – пропали данные»: новая версия должна читать JSON, сохранённый старой.
 * Если тест падает после изменения StoredModels.kt – поменяли формат хранения, нужна миграция.
 */
class StoredModelsTest {
    private val gson = Gson()

    @Test
    fun `json saved by previous app version is still readable`() {
        // Ровно такой JSON писала прежняя версия (Gson по именам полей OnboardingAnswers)
        val oldJson = """{"userName":"Мама","childGender":"BOY","isItABoy":"YES",""" +
                """"childName":"Ваня","childBirthDate":"2020-05-19","childHobbies":"chess, books"}"""

        val answers = gson.fromJson(oldJson, OnboardingAnswersDto::class.java).toDomain()

        assertEquals(
            OnboardingAnswers("Мама", "BOY", "YES", "Ваня", "2020-05-19", "chess, books"),
            answers
        )
    }

    @Test
    fun `missing and null keys become empty strings, not null`() {
        val answers =
            gson.fromJson("""{"userName":null}""", OnboardingAnswersDto::class.java).toDomain()
        assertEquals(OnboardingAnswers(), answers)
    }

    @Test
    fun `round trip keeps json keys stable`() {
        val answers = OnboardingAnswers("Мама", "GIRL", "", "Маша", "2019-02-28", "drawing")
        val json = gson.toJson(answers.toDto())

        assertEquals(true, json.contains("\"childBirthDate\":\"2019-02-28\""))
        assertEquals(answers, gson.fromJson(json, OnboardingAnswersDto::class.java).toDomain())
    }

    @Test
    fun `birthday notification data is readable from old json`() {
        val data = gson.fromJson(
            """{"childName":"Ваня","childBirthDate":"2006-09-15"}""",
            BirthdayNotificationDto::class.java
        )
        assertEquals("2006-09-15", data.toDomain().childBirthDate)
    }

    @Test
    fun `hobbies are read from both old string and new array format`() {
        assertEquals("chess, books", UserFirestoreMapper.hobbiesFromField("chess, books"))
        assertEquals("chess, books", UserFirestoreMapper.hobbiesFromField(listOf("chess", "books")))
        assertEquals(listOf("chess", "books"), UserFirestoreMapper.hobbiesToList("chess, books"))
        assertEquals(emptyList<String>(), UserFirestoreMapper.hobbiesToList(""))
    }
}