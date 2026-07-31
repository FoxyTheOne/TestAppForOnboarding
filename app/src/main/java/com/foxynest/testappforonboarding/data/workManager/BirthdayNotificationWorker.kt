package com.foxynest.testappforonboarding.data.workManager

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foxynest.testappforonboarding.domain.use_cases.authUseCases.IGetCurrentUserPresentationUC
import com.foxynest.testappforonboarding.other.Status
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Отвечает за планирование работы:
 *     Создаёт PeriodicWorkRequest с нужными параметрами (интервал, ограничения, политика замены).
 *     Отвечает за отмену (cancel) запланированной работы.
 *     Является сервисным слоем (Data layer), инкапсулирующим работу с WorkManager.
 */
@HiltWorker
class BirthdayNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getCurrentUserUC: IGetCurrentUserPresentationUC,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userResult = getCurrentUserUC.invoke()
        if (userResult.status != Status.SUCCESS) {
            return Result.failure()
        }

        val user = userResult.data ?: return Result.failure()
        if (user.childName.isBlank() || user.childBirthDate.isBlank()) {
            return Result.failure()
        }

        return if (isTodayBirthday(user.childBirthDate)) {
            notificationHelper.showBirthdayNotificationIfPermitted(user.childName)
            Result.success()
        } else {
            // Не день рождения – ничего не делаем, но возвращаем success, чтобы WorkManager не перезапускал с ошибкой
            Result.success()
        }
    }

    private fun isTodayBirthday(birthDate: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = formatter.parse(birthDate) ?: return false
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            calendar.time = date
            today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): BirthdayNotificationWorker
    }
}

/**
 * Почему @AssistedFactory внутри класса Worker?
 *
 * Это стандартная практика для Hilt + WorkManager. Worker создаётся не через DI-контейнер напрямую, а через фабрику, потому что WorkManager сам создаёт экземпляры Worker, передавая Context и WorkerParameters. Hilt не может автоматически внедрить зависимости в конструктор Worker, потому что эти параметры передаются во время выполнения. Поэтому мы:
 *     Помечаем конструктор @AssistedInject.
 *     Параметры context и params помечаем @Assisted.
 *     Создаём внутренний интерфейс Factory с методом create, который принимает Context и WorkerParameters, а остальные зависимости (например, getCurrentUserUC, notificationHelper) Hilt будет внедрять автоматически при создании фабрики.
 *
 * Почему не в отдельном модуле?
 * Фабрика тесно связана с конкретным Worker – она знает, какие параметры нужны для его создания. Обычно её объявляют внутри класса Worker (или рядом) для удобства и читаемости. В Dagger/Hilt это распространённый паттерн. Вы можете вынести её в отдельный файл, но это не обязательно и может усложнить навигацию. Оставляйте внутри – это правильно.
 */