package com.foxynest.testappforonboarding

/**
 * Простые модульные тесты для основных компонентов системы уведомлений о дне рождения.
 * Тесты написаны на Kotlin с использованием JUnit4, MockK и корутин.
 * Они покрывают логику расчёта дат, сохранения данных, планирования работы и выполнения воркера.
 */
import android.app.AlarmManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import com.foxynest.testappforonboarding.data.dataSource.datastorePreferences.IAppDatastorePreferences
import com.foxynest.testappforonboarding.data.repository.BirthdayNotificationRepository
import com.foxynest.testappforonboarding.data.workManager.BirthdayNotificationWorker
import com.foxynest.testappforonboarding.domain.entities.BirthdayNotificationData
import com.foxynest.testappforonboarding.domain.entities.isValidData
import com.foxynest.testappforonboarding.domain.iRepository.IBirthdayNotificationRepository
import com.foxynest.testappforonboarding.other.timeProvider.ITimeProvider
import com.foxynest.testappforonboarding.other.utils.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BirthdayNotificationTests {

    // Моки зависимостей
    private val mockWorkManager: WorkManager = mockk()
    private val mockNotificationHelper: NotificationHelper = mockk()
    private val mockDataStore: IAppDatastorePreferences = mockk()
    private val mockTimeProvider: ITimeProvider = mockk()
    private val mockAlarmManager: AlarmManager = mockk()
    private val mockContext: Context = mockk()
    private val mockBirthdayNotificationRepository: IBirthdayNotificationRepository = mockk()

    // Переменные
    private lateinit var repository: BirthdayNotificationRepository
    private lateinit var worker: BirthdayNotificationWorker
//
//    @Before
//    fun setup() {
//        // Создаём репозиторий с моками
//        repository = BirthdayNotificationRepository(
////            workManager = mockWorkManager,
//            notificationHelper = mockNotificationHelper,
////            timeProvider = mockTimeProvider,
//            context = mockContext,
//            dataStore = mockDataStore
//        )
//
//        // Для тестов воркера создаём его с моками (используем Assisted Injection, но для тестов проще создать через конструктор)
//        worker = BirthdayNotificationWorker(
//            context = mockContext,
//            workerParams = mockk(),
//            dataStore = mockDataStore,
//            notificationHelper = mockNotificationHelper,
//            birthdayNotificationRepository = mockBirthdayNotificationRepository
////            timeProvider = mockTimeProvider
//        )
//
////            // Инициализируем WorkManager для тестов (если нужно)
////            val config = Configuration.Builder()
////                .setMinimumLoggingLevel(android.util.Log.DEBUG)
////                .build()
////            WorkManager.initialize(mockContext, config)
//    }
//
//    // 1. Тест валидации данных
//    @Test
//    fun validBirthDataTest() {
//        val validData = BirthdayNotificationData("Alice", "2020-01-01")
//        assertEquals(
//            "birthdayNotificationData.isValidData() must return true when name and date are valid",
//            true, validData.isValidData()
//        )
//    }
//
//    @Test
//    fun notValidChildNameTest() {
//        val invalidData1 = BirthdayNotificationData("", "2020-01-01")
//        val invalidData2 = BirthdayNotificationData("null", "2020-01-01")
//        assertEquals(
//            "birthdayNotificationData. isValidData() must return false when name is empty",
//            false, invalidData1.isValidData()
//        )
//        assertEquals(
//            "birthdayNotificationData. isValidData() must return false when name is null",
//            false, invalidData2.isValidData()
//        )
//    }
//
//    @Test
//    fun notValidBirthDataTest() {
//        val invalidData1 = BirthdayNotificationData("Alice", "")
//        val invalidData2 = BirthdayNotificationData("Alice", "null")
//        assertEquals(
//            "birthdayNotificationData.isValidData() must return false when date is empty",
//            false, invalidData1.isValidData()
//        )
//        assertEquals(
//            "birthdayNotificationData.isValidData() must return false when date is null",
//            false, invalidData2.isValidData()
//        )
//    }
//
//    // 2. Тест проверки, сегодня ли день рождения
//    @Test
//    fun birthdayIsTodayTest() {
//        // Фиксируем сегодняшнюю дату (например, 1 января 2025)
//        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2025-01-01")!!
//        val birthDate = "2025-01-01"
//        assertEquals(
//            "isTodayBirthday() must return true when birth date matches today",
//            true, birthDate.isBirthdayToday(today)
//        )
//    }
//
//    @Test
//    fun birthdayIsNotTodayTest() {
//        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2025-01-01")!!
//        val birthDate = "2025-01-02"
//        assertEquals(
//            "isTodayBirthday() must return false when birth date is not today",
//            false, birthDate.isBirthdayToday(today)
//        )
//    }
//
//    // 3. Тест расчёта задержки до следующего утра
//    @Test
//    fun correctDelayBefore9am() {
//        // Сейчас 8:00 утра
//        val now = Calendar.getInstance().apply {
//            set(2025, Calendar.JANUARY, 1, 8, 0, 0)
//            set(Calendar.MILLISECOND, 0)
//        }.time
//
//        val delay = now.calculateDelayToNextMorning()
//        // Ожидаем разницу между 9:00 и 8:00 = 1 час (3600000 мс)
//        assertEquals(
//            "calculateWorkerDelayToNextMorning() must return correct delay when before 9am",
//            3600000L, delay
//        )
//    }
//
//    @Test
//    fun correctDelaAfter9am() {
//        // Сейчас 10:00 утра
//        val now = Calendar.getInstance().apply {
//            set(2025, Calendar.JANUARY, 1, 10, 0, 0)
//            set(Calendar.MILLISECOND, 0)
//        }.time
//
//        val delay = now.calculateDelayToNextMorning()
//        // Ожидаем до завтрашнего 9:00 = 23 часа (82800000 мс)
//        assertEquals(
//            "calculateWorkerDelayToNextMorning() must return correct delay when after 9am",
//            82800000L,
//            delay
//        )
//    }
//
////    // 4. Тест планирования: сохранение данных и замена работы
////    // Проверяет, что репозиторий сохраняет данные, отменяет старую работу и создаёт новую с правильной задержкой
////    // НЕПРАВИЛЬНЫЙ ТЕСТ, не проходит
////    @Test
////    fun savingDataAndEnqueuingUniquePeriodicWorkTest() = runTest {
////        // Подготовка: timeProvider возвращает фиксированное время (8:00)
////        every { mockTimeProvider.getCurrentTime() } returns Calendar.getInstance().apply {
////            set(2025, Calendar.JANUARY, 1, 8, 0, 0)
////        }.time
////
////        // Мокаем WorkManager
////        coEvery { mockDataStore.saveBirthdayNotificationData(any()) } returns Unit
////        coEvery { mockDataStore.clearBirthdayNotificationData() } returns Unit
////        mockkStatic(WorkManager::class)
////        every { WorkManager.getInstance(any<Context>()) } returns mockWorkManager
////        every { mockWorkManager.cancelUniqueWork(any()) } returns mockk()
////        val mockOperation = mockk<Operation>(relaxed = true)
////        every {
////            mockWorkManager.enqueueUniquePeriodicWork(
////                any(),
////                any(),
////                any()
////            )
////        } returns mockOperation
////
////        // Действие
////        repository.scheduleBirthdayNotification("Bob", "2025-01-01")
////
////        // Проверка: сохранены данные
////        coVerify {
////            mockDataStore.saveBirthdayNotificationData(
////                BirthdayNotificationData(
////                    "Bob",
////                    "2025-01-01"
////                )
////            )
////        }
////        // Проверка: отмена старой работы
////        verify { mockWorkManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME) }
////
////        // Проверка: создание периодической работы
////        val captor = slot<PeriodicWorkRequest>()
////        verify {
////            mockWorkManager.enqueueUniquePeriodicWork(
////                BIRTHDAY_NOTIF_UNIQUE_WORK_NAME,
////                ExistingPeriodicWorkPolicy.REPLACE,
////                capture(captor)
////            )
////        }
////
////        // Проверяем начальную задержку (8:00 -> 9:00 = 1 час)
////        assertEquals(
////            "Начальная задержка должна быть равна 1 час (8:00 -> 9:00 = 1 час)",
////            3600000L, captor.captured.workSpec.initialDelay
////        )
////    }
//
//    // 5. Тест отмены уведомления
////    @Test
////    fun clearingDataAndCancelingWorkTest() = runTest {
////        Log.d(
////            "BirthdayNotificationTests",
////            "cancelBirthdayNotification() must clear data and cancels work"
////        )
////        coEvery { mockDataStore.clearBirthdayNotificationData() } returns Unit
////        every { mockWorkManager.cancelUniqueWork(any()) } returns mockk()
////
////        repository.cancelBirthdayNotification()
////
////        coVerify { mockDataStore.clearBirthdayNotificationData() }
////        verify { mockWorkManager.cancelUniqueWork(BIRTHDAY_NOTIF_UNIQUE_WORK_NAME) }
////    }
//
//    // 6. Тест воркера: день рождения сегодня → показываем уведомление
//    @Test
//    fun showingNotificationWhenTodayIsBirthdayTest() = runTest {
//        // Подготовка данных: день рождения сегодня
//        val today = Calendar.getInstance().apply {
//            set(2025, Calendar.JANUARY, 1, 10, 0, 0)
//        }.time
//        every { mockTimeProvider.getCurrentTime() } returns today
//
//        val data = BirthdayNotificationData("Alice", "2025-01-01")
//        coEvery { mockDataStore.getBirthdayNotificationDataFlow() } returns flowOf(data)
//        every { mockNotificationHelper.showBirthdayNotificationIfPermitted(any()) } returns Unit
//
//        val result = worker.doWork()
//
//        assertEquals(
//            "День рождения сегодня, мы должны получить success",
//            ListenableWorker.Result.success(), result
//        )
//        verify { mockNotificationHelper.showBirthdayNotificationIfPermitted("Alice") }
//    }
//
//    // 7. Тест воркера: день рождения не сегодня → не показываем уведомление
//    @Test
//    fun notShowingNotificationWhenTodayIsNotBirthdayTest() = runTest {
//        val today = Calendar.getInstance().apply {
//            set(2025, Calendar.JANUARY, 2, 10, 0, 0)
//        }.time
//        every { mockTimeProvider.getCurrentTime() } returns today
//
//        val data = BirthdayNotificationData("Alice", "2025-01-01")
//        coEvery { mockDataStore.getBirthdayNotificationDataFlow() } returns flowOf(data)
//        // Уведомление не должно вызываться
//        every { mockNotificationHelper.showBirthdayNotificationIfPermitted(any()) } returns Unit
//
//        val result = worker.doWork()
//
//        assertEquals(
//            "Result must be success but without notification",
//            ListenableWorker.Result.success(),
//            result
//        )
//        verify(exactly = 0) { mockNotificationHelper.showBirthdayNotificationIfPermitted(any()) }
//    }
//
//    // 8. Тест воркера: невалидные данные → возвращаем failure
//    @Test
//    fun invalidWorkerDataTest() = runTest {
//        val data = BirthdayNotificationData() // пустые поля
//        coEvery { mockDataStore.getBirthdayNotificationDataFlow() } returns flowOf(data)
//
//        val result = worker.doWork()
//
////        assertEquals(
////            "worker must return failure when data is invalid",
////            ListenableWorker.Result.failure(), result
////        )
//        assertEquals(
//            "worker must return success even when data is invalid",
//            ListenableWorker.Result.success(), result
//        )
//        // Уведомление не вызывается
//        verify(exactly = 0) { mockNotificationHelper.showBirthdayNotificationIfPermitted(any()) }
//    }
//
////    // 9. Тест UseCase (если хотим проверить, что он вызывает репозиторий)
////    // Проверяет, что он вызывает репозиторий при валидных данных и отменяет при невалидных
////    @Test
////    fun scheduleNotificationWithValidParametersTest() = runTest {
////        val mockRepo: IBirthdayNotificationRepository = mockk()
////        val useCase = ScheduleBirthdayNotificationUC(mockRepo)
////        coEvery { mockRepo.scheduleBirthdayNotification(any(), any()) } returns Unit
////
////        useCase.scheduleBirthdayNotification("Alice", "2025-01-01")
////
////        coVerify { mockRepo.scheduleBirthdayNotification("Alice", "2025-01-01") }
////    }
////
////    @Test
////    fun cancelingNotificationWithNotValidParametersTest() = runTest {
////        val mockRepo: IBirthdayNotificationRepository = mockk()
////        val useCase = ScheduleBirthdayNotificationUC(mockRepo)
////        coEvery { mockRepo.cancelBirthdayNotification() } returns Unit
////
////        useCase.scheduleBirthdayNotification("", "2025-01-01")
////
////        coVerify { mockRepo.cancelBirthdayNotification() }
////    }
////
////    // 10. Тест checkAndNotifyIfBirthdayToday в репозитории. Проверяет делегирование в NotificationHelper
////    @Test
////    fun repositoryCheckAndNotifyIfBirthdayTodayDelegatesToHelper() = runTest {
////        val today = Date()
////        every { mockTimeProvider.getCurrentTime() } returns today
////        every {
////            mockNotificationHelper.checkAndNotifyIfBirthdayToday(
////                any(),
////                any(),
////                any()
////            )
////        } returns Unit
////
////        repository.checkAndNotifyIfBirthdayToday("Alice", "2025-01-01")
////
////        verify {
////            mockNotificationHelper.checkAndNotifyIfBirthdayToday(
////                "Alice",
////                "2025-01-01",
////                today
////            )
////        }
////    }

//    @Test
//    fun testBirthdayNotificationRepeatsYearly() = runTest {
//        // Подготовка
//        val fakeTime =
//            FakeTimeProvider(Date(2026, Calendar.MARCH, 10, 10, 0)) // 10 марта 2026 10:00
//        val repository = BirthdayNotificationRepository(
//            context = mockContext,
//            alarmManager = mockAlarmManager,
//            appDatastorePreferences = mockDataStore,
//            timeProvider = fakeTime,
//            notificationHelper = mockNotificationHelper
//        )
//        // Задаём дату рождения 10 марта
//        repository.scheduleBirthdayNotification("Child", "2026-03-10")
//
//        // Проверяем, что будильник запланирован на следующий год (10 марта 2027 в 9:00)
//        val expectedTrigger = Date(2027, Calendar.MARCH, 10, 9, 0).time
//        verify(mockAlarmManager).setWindow(eq(AlarmManager.RTC), eq(expectedTrigger), anyLong(), any())
//
//        // Перематываем время на 10 марта 2027 9:05
//        fakeTime.advanceDays(365) // или более точно
//        fakeTime.currentTime = Date(2027, Calendar.MARCH, 10, 9, 5)
//
//        // Имитируем срабатывание Worker
//        val worker = BirthdayNotificationWorker(
//            context = mockContext,
//            params = mockWorkerParameters,
//            appDatastorePreferences = mockDataStore,
//            notificationHelper = mockNotificationHelper,
//            timeProvider = fakeTime,
//            birthdayNotificationRepository = repository
//        )
//        worker.doWork()
//
//        // Проверяем, что уведомление показано ещё раз, и запланирован 2028 год
//        verify(mockNotificationHelper, times(2)).showBirthdayNotificationIfPermitted("Child", "2026-03-10")
//        val expectedNextTrigger = Date(2028, Calendar.MARCH, 10, 9, 0).time
//        verify(mockAlarmManager, times(2)).setWindow(eq(AlarmManager.RTC), eq(expectedNextTrigger), anyLong(), any())
//    }

}