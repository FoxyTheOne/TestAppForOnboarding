package com.foxynest.testappforonboarding.other.constants

/**
 * Общие константы.
 * HOBBIES_DELIMITER – разделитель хобби в строке «chess, books». Менять нельзя: так уже сохранены анкеты у пользователей.
 * DATASTORE_NAME – имя файла DataStore на устройстве. Тоже менять нельзя: новый файл = все локальные данные «пропадут».
 */
object CommonConstants {
    const val PAY_URL = "https://boosty.to/foxynest/donate"
    const val DATASTORE_NAME = "app_datastore_preferences"
    const val HOBBIES_DELIMITER = ", "

    // Destination for navigation
    const val PROFILE_DESTINATION = "Profile"

    // Firebase
    const val USERS_COLLECTION = "users"
}