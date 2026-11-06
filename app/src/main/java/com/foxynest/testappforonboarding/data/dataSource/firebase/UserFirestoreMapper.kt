package com.foxynest.testappforonboarding.data.dataSource.firebase

import com.foxynest.testappforonboarding.domain.entities.user.UserPresentation
import com.foxynest.testappforonboarding.other.constants.CommonConstants.HOBBIES_DELIMITER
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

/**
 * Преобразование профиля в документ Firestore и обратно. Живёт в data-слое: только он знает о Firestore.
 *
 * Документ Firestore – это набор пар «имя поля → значение» (как Map), поэтому пишем Map,
 * а читаем по именам полей из [UserFirestoreFields]. Имена полей – часть «контракта» с правилами безопасности:
 * поле, которого нет в `allowedFields()` в `firestore.rules`, сервер не примет.
 */
object UserFirestoreMapper {

    /** Хобби пишутся массивом – по нему можно искать и валидировать в правилах безопасности. */
    fun toFirestoreMap(user: UserPresentation): Map<String, Any> = mapOf(
        UserFirestoreFields.USERNAME to user.userName,
        UserFirestoreFields.CHILD_GENDER to user.childGender,
        UserFirestoreFields.CHILD_IS_IT_A_BOY to user.isItABoy,
        UserFirestoreFields.CHILD_NAME to user.childName,
        UserFirestoreFields.CHILD_HOBBIES to hobbiesToList(user.childHobbies),
        UserFirestoreFields.CHILD_BIRTH_DATE to user.childBirthDate
    )

    /**
     * `FieldValue.serverTimestamp()` – не время телефона, а «метка»: сервер сам подставит своё время при записи.
     * Часы на телефоне могут быть сбиты, серверным – можно доверять.
     */
    fun toFirestoreMapForCreate(user: UserPresentation): Map<String, Any> =
        toFirestoreMap(user) + (UserFirestoreFields.CREATED_AT to FieldValue.serverTimestamp())

    fun toFirestoreMapForUpdate(user: UserPresentation): Map<String, Any> =
        toFirestoreMap(user) + (UserFirestoreFields.UPDATED_AT to FieldValue.serverTimestamp())

    /** uid, почта и «подтверждена ли» приходят из Auth – в документе Firestore их нет. */
    fun fromDocument(
        document: DocumentSnapshot,
        uid: String,
        authEmail: String,
        isEmailVerified: Boolean
    ): UserPresentation = UserPresentation(
        uid = uid,
        authEmail = authEmail,
        isEmailVerified = isEmailVerified,
        userName = document.stringField(UserFirestoreFields.USERNAME),
        childGender = document.stringField(UserFirestoreFields.CHILD_GENDER),
        isItABoy = document.stringField(UserFirestoreFields.CHILD_IS_IT_A_BOY),
        childName = document.stringField(UserFirestoreFields.CHILD_NAME),
        childHobbies = hobbiesFromField(document.get(UserFirestoreFields.CHILD_HOBBIES)),
        childBirthDate = document.stringField(UserFirestoreFields.CHILD_BIRTH_DATE)
    )

    /** «chess, books» → ["chess", "books"]. `internal` – виден в тестах этого модуля, но не снаружи. */
    internal fun hobbiesToList(hobbies: String): List<String> =
        hobbies.split(HOBBIES_DELIMITER).map { it.trim() }.filter { it.isNotEmpty() }

    /** Старые документы хранят хобби строкой через запятую, новые – массивом. Читаем оба формата. */
    internal fun hobbiesFromField(value: Any?): String = when (value) {
        is List<*> -> value.filterIsInstance<String>().joinToString(HOBBIES_DELIMITER)
        is String -> value
        else -> ""
    }

    // getString() бросает исключение, если в поле лежит не строка – один «кривой» документ ронял бы загрузку профиля
    private fun DocumentSnapshot.stringField(field: String): String = get(field) as? String ?: ""
}