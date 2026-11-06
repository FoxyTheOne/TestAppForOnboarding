# Указатель по коду: какая задача где решена

Слева задача, справа файл, где она решена. У каждого класса есть комментарий (KDoc): зачем он нужен и на что обратить внимание. Это мой конспект: здесь искать, «как я это уже делала».

Подробные разборы сложных тем:

- [docs/BIRTHDAY_NOTIFICATION.md](docs/BIRTHDAY_NOTIFICATION.md) – уведомление о дне рождения:
  правила, схема, почему так, как проверить;
- [docs/FIREBASE.md](docs/FIREBASE.md) – Firebase: кто что хранит, все сценарии по шагам, безопасность и ответственность;
- [docs/FIRESTORE_RULES.md](docs/FIRESTORE_RULES.md) – правила безопасности Firestore: как читать и что менять при изменениях в программе.
- [docs/BEFORE_REAL_APP.md](docs/BEFORE_REAL_APP.md) – что решить и сделать перед настоящим (платным) приложением.

Пути указаны от `app/src/main/java/com/foxynest/testappforonboarding/`.

## Как устроен проект: слои

```
 presentation  – экраны, ViewModel, диалоги. Знает про Android.
      │ вызывает
      ▼
 domain        – use case-ы, модели, интерфейсы репозиториев. Чистый Kotlin, без Android и без Firebase.
      ▲ реализует интерфейсы
      │
 data          – репозитории, DataStore, Firebase, AlarmManager. Знает «как», но не «зачем».
```

- **Use case
  ** – одна операция бизнес-логики («войти», «проверить почту», «удалить аккаунт»), обычно одна функция
  `execute()`/
  `invoke()`. Он стоит между ViewModel и data-слоем и работает только через интерфейсы репозиториев. Отдельный класс на каждое действие – это принцип единственной ответственности: легко найти, легко протестировать.
- **Интерфейс рядом с каждым use case-ом** (`ILoginUC` и
  `LoginUC`) нужен для тестов: в тесте ViewModel вместо настоящего use case-а подставляется муляж (
  `mockk<ILoginUC>()`).
- **Интерфейсы репозиториев – в domain, реализации – в data** (инверсия зависимостей). Domain просит
  `IAuthRepository`, а Hilt по `@Binds` подставляет `AuthRepository`.
- **ViewModel
  ** вызывает use case-ы и превращает их результаты в состояние для экрана. Текст ошибок она не выбирает:
  в состоянии лежит `ErrorType`, а текст на нужном языке подбирает фрагмент.

## Настройка проекта

| Задача                                                                                        | Где смотреть                                                                     |
|-----------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Hilt: точка входа приложения                                                                  | `App.kt` (`@HiltAndroidApp`), манифест `android:name=".App"`                     |
| Hilt: `@Provides` для чужих классов и `@Binds` «интерфейс → реализация»                       | `di/SingletonModule.kt`                                                          |
| Hilt: зависимости, которые живут столько же, сколько ViewModel                                | `di/ViewModelModule.kt`                                                          |
| Hilt в BroadcastReceiver через EntryPoint                                                     | `data/notification/BirthdayNotificationReceiver.kt` (`ReceiverEntryPoint`)       |
| R8 (minifyEnabled) не ломает сохранённые данные: `@SerializedName`, `@Keep`, правило proguard | `data/dataSource/datastorePreferences/StoredModels.kt`, `app/proguard-rules.pro` |
| Подменяемые «часы» для тестов                                                                 | `other/timeProvider/ITimeProvider.kt`, `SystemTimeProvider.kt`                   |

## Данные на устройстве

| Задача                                                                               | Где смотреть                                                                                            |
|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| DataStore: один экземпляр на файл (top-level делегат), защита от повреждённого файла | `data/dataSource/datastorePreferences/AppDatastorePreferences.kt` (начало файла)                        |
| DataStore: атомарная запись нескольких ключей (`edit { }` как транзакция)            | там же, `commitOnboardingDraft`, `updateOnboardingDraft`                                                |
| Объект в DataStore как JSON (Gson), отдельные модели хранения                        | `StoredModels.kt` + `readJson` / `writeJson`                                                            |
| Черновик опроса: отмена посреди опроса не портит сохранённую анкету                  | `data/repository/OnboardingRepository.kt`, `MainViewModel` (`prepareForOnboarding`, `finishOnboarding`) |
| Запись строго по очереди (Mutex), чтобы последний ответ не потерялся                 | `presentation/MainViewModel.kt` (`localDataMutex`, `launchLocalWrite`)                                  |
| Миграция данных при смене формата хранения (версия схемы)                            | `data/repository/MigrateUserDataRepository.kt`, `domain/entities/DataMigrationHelper.kt`                |
| Хранить ключи (`BOY`, `YES`, `chess`), а текст подбирать по языку при показе         | `domain/entities/Gender.kt`, `onboarding/YesNoAnswer.kt`, `presentation/common/DomainDisplay.kt`        |
| Дата строкой `yyyy-MM-dd` и показ по языку телефона                                  | `other/extensions/DateExtensions.kt`, `ProfileViewModel.formatBirthDateForDisplay`                      |

## Firebase (подробно – [docs/FIREBASE.md](docs/FIREBASE.md))

| Задача                                                                                   | Где смотреть                                                                                                                                      |
|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Регистрация: аккаунт → документ профиля → письмо подтверждения                           | `data/dataSource/firebase/FirebaseAuthDataSource.kt` (`register`)                                                                                 |
| Вход, выход с проверкой неотправленных изменений                                         | там же, `login`, `logout`, `awaitPendingWrites`                                                                                                   |
| Подписка на документ (snapshot-листенер) через `callbackFlow` + `awaitClose`             | там же, `profileFlow`                                                                                                                             |
| Подписка на вход/выход и переключение листенера на нового пользователя (`flatMapLatest`) | там же, `authStateFlow`, `observeCurrentUser`                                                                                                     |
| Flow → StateFlow с отключением без подписчиков (`stateIn` + `WhileSubscribed`)           | `presentation/MainViewModel.kt` (`checkedUserInfo`)                                                                                               |
| Запись без сети: ждём сервер ограниченное время                                          | `FirebaseAuthDataSource.awaitWrite`                                                                                                               |
| Обновить только переданные поля (`SetOptions.merge`), время сервера (`serverTimestamp`)  | `updateProfile`, `UserFirestoreMapper.kt`                                                                                                         |
| Не перезаписать профиль на сервере пустыми полями                                        | `domain/use_cases/updatingDataUseCases/UpdateFirebaseProfileUC.kt`                                                                                |
| Досылка профиля, если запись после регистрации не удалась                                | `MainViewModel.repairFirebaseProfileFromLocalData`                                                                                                |
| Удаление аккаунта: повторный вход → документ → аккаунт → локальные данные                | `FirebaseAuthDataSource.deleteAccount`, `domain/use_cases/authUseCases/account/DeleteAccountUC.kt`, `presentation/dialogs/DeleteAccountDialog.kt` |
| Смена почты для входа (`verifyBeforeUpdateEmail`) и пояснение «войдите заново»           | `updateAuthEmail`, `presentation/dialogs/ChangeAuthEmailDialog.kt`                                                                                |
| Заметить, что Firebase сам завершил сессию                                               | `MainViewModel.observeSessionEnd`, `MainActivity` (`sessionEndedEvent`)                                                                           |
| Мягкое подтверждение почты + проверка статуса (`reload`) с ограниченным опросом          | `checkEmailVerified`, `presentation/profile/ProfileViewModel.kt`                                                                                  |
| Сброс пароля с нейтральным сообщением                                                    | `sendPasswordResetEmail`, `account/SendPasswordResetUC.kt`, `LoginFragment`                                                                       |
| Ошибки Firebase → тип ошибки → текст на нужном языке                                     | `FirebaseAuthDataSource.toErrorResource`, `domain/entities/ErrorType.kt`, `presentation/common/DomainDisplay.kt`                                  |
| Чтение двух форматов поля (строка и массив)                                              | `UserFirestoreMapper.hobbiesFromField`                                                                                                            |
| Правила безопасности                                                                     | `firestore.rules` (корень проекта), [docs/FIRESTORE_RULES.md](docs/FIRESTORE_RULES.md)                                                            |

## Уведомление о дне рождения (подробно – [docs/BIRTHDAY_NOTIFICATION.md](docs/BIRTHDAY_NOTIFICATION.md))

| Задача                                                                                              | Где смотреть                                                                                                               |
|-----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Логика дат без Android: сегодня ли праздник, когда поздравлять, 29 февраля                          | `domain/birthday/BirthdayRules.kt`                                                                                         |
| Неточный будильник без пробуждения (`setWindow` + `RTC`), один PendingIntent для установки и отмены | `data/repository/BirthdayNotificationRepository.kt`                                                                        |
| Восстановление будильника после перезагрузки, смены времени, обновления приложения                  | `data/notification/BirthdayNotificationReceiver.kt` + `AndroidManifest.xml`                                                |
| Короткая фоновая работа в ресивере (`goAsync` + таймаут)                                            | `BirthdayNotificationReceiver.onReceive`                                                                                   |
| Не поздравить дважды (ключ «уже поздравили» + Mutex)                                                | `BirthdayRules.occurrenceKey`, `BirthdayNotificationRepository`                                                            |
| Уведомление: канал, язык приложения в фоне, версия для экрана блокировки                            | `data/notification/NotificationHelper.kt`                                                                                  |
| Разрешение на уведомления в контексте, с объяснением и один раз                                     | `MainViewModel.askNotificationPermissionIfNeeded`, `MainActivity`, `dialogs/simpleDialogs/NotificationPermissionDialog.kt` |

## Экраны и диалоги

| Задача                                                                                 | Где смотреть                                                                                        |
|----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Одна ViewModel на все экраны (`activityViewModels`)                                    | `presentation/MainViewModel.kt`, любой фрагмент                                                     |
| Состояние vs одноразовое событие (MyEvent)                                             | `other/MyEvent.kt`, `MainViewModel` (`...State` и `...Event`)                                       |
| Результат операции или загрузки со статусом                                            | `other/Resource.kt`                                                                                 |
| Подписка на StateFlow по жизненному циклу экрана (`repeatOnLifecycle`)                 | `presentation/profile/ProfileFragment.kt` (`collectWhileStarted`)                                   |
| Форма: события от экрана → состояние с ошибками полей                                  | `presentation/welcome/auth/login/` (`LoginFormEvent`, `LoginFormState`, `LoginViewModel`)           |
| Диалог «Да/Нет», переживающий пересоздание экрана (Fragment Result API)                | `presentation/dialogs/simpleDialogs/BaseDialogFragment.kt` + наследники                             |
| Диалог с полем ввода, который не закрывается при ошибке, и состояние в общей ViewModel | `presentation/dialogs/ChangeAuthEmailDialog.kt`, `DataSaveErrorDialog.kt`, `DeleteAccountDialog.kt` |
| Безопасный переход между экранами (двойное нажатие не роняет приложение)               | `other/extensions/FragmentExtensions.kt`                                                            |
| Кнопка «Назад»: запрет в опросе, вопрос «выйти?» на главных экранах                    | `presentation/MainActivity.kt`                                                                      |
| Нижнее меню прячется на экранах опроса; прогресс опроса                                | `MainActivity.setupNavigation`, `welcome/onboardingAndPaywall/OnboardingViewModel.kt`               |
| Стартовый экран зависит от данных (вошёл / прошёл опрос)                               | `presentation/splash/SplashViewModel.kt`                                                            |

## Ввод данных

| Задача                                                                                    | Где смотреть                                                        |
|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Имя: буквы любого алфавита, пробел, дефис, апостроф, лимит длины как в правилах Firestore | `domain/entities/PersonNameRules.kt`                                |
| Фильтр символов в поле ввода (`InputFilter`) вместо `android:digits`                      | `presentation/common/PersonNameInputFilter.kt`, `Question1Fragment` |
| Ошибка под полем: показать при «Далее», убрать при вводе                                  | `Question1Fragment.validateName`                                    |
| Выбор даты (MaterialDatePicker) без сдвига на день из-за часовых поясов, без будущих дат  | `welcome/onboardingAndPaywall/Question3Fragment.kt`                 |
| Выбранная дата и слушатель календаря переживают пересоздание экрана                       | там же, `onSaveInstanceState`, `restorePickerListener`              |
| Чипы из массива ключей с восстановлением выбора                                           | `welcome/onboardingAndPaywall/Question4Fragment.kt`                 |
| Проверка почты без Android (`Regex` вместо `Patterns`)                                    | `other/utils/EmailUtils.kt`                                         |
| Поле, которое прячет клавиатуру при потере фокуса                                         | `other/utils/BaseTextInputEditText.kt`                              |

## Язык приложения

| Задача                                                                                    | Где смотреть                                                                                |
|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| Переключение языка через AppCompat (`setApplicationLocales`), без `runBlocking` на старте | `presentation/language/LanguageManager.kt`, манифест (`autoStoreLocales`)                   |
| Одноразовый перенос языка из старой версии                                                | `LanguageManager.migrateLegacyLanguageIfNeeded`, `data/repository/AppLanguageRepository.kt` |
| Текст в фоне (уведомление) на языке приложения, а не системы                              | `NotificationHelper.withAppLanguage`                                                        |

## Тесты

Unit-тесты – `app/src/test/...` (на компьютере, быстро). Instrumented – `app/src/androidTest/...` (
на эмуляторе).

| Задача                                                                             | Где смотреть                                                                     |
|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Тест ViewModel: подмена `Dispatchers.Main` и виртуальное время                     | `test/.../MainDispatcherRule.kt` (подробное объяснение внутри)                   |
| LiveData в unit-тесте (`InstantTaskExecutorRule`)                                  | `test/.../MainViewModelTests.kt`                                                 |
| Муляжи (mockk): `every`/`coEvery`, `coAnswers`, `coVerify`                         | `MainViewModelTests.kt` (словарик в KDoc), `AccountUseCasesTest.kt`              |
| Проверка порядка операций (медленная запись + завершение опроса)                   | `MainViewModelTests` → `finish onboarding waits for the last answer to be saved` |
| Перемотка времени (`advanceTimeBy`, `runCurrent`)                                  | `test/.../ProfileViewModelTests.kt`                                              |
| Логика дат по сценариям задачи                                                     | `test/.../domain/birthday/BirthdayRulesTest.kt`                                  |
| Новая версия читает данные старой (JSON)                                           | `test/.../data/StoredModelsTest.kt`                                              |
| Тест на устройстве с настоящими AlarmManager и уведомлениями + фиксированное время | `androidTest/.../BirthdayNotificationRepositoryTest.kt`                          |
| Тест-«напоминалка», что лимит совпадает с правилами Firestore                      | `test/.../domain/entities/PersonNameRulesTest.kt`                                |
