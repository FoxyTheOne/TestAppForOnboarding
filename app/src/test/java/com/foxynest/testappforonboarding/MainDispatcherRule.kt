package com.foxynest.testappforonboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Подмена главного потока Android для unit-тестов ViewModel.
 *
 * Проблема: `viewModelScope.launch { }` запускает код на `Dispatchers.Main` – главном потоке Android.
 * Unit-тесты (папка `test`) работают на обычной Java-машине компьютера, Android там нет, и первый же
 * launch падает с «Module with the Main dispatcher is missing».
 *
 * Решение – правило (Rule) JUnit: JUnit сам вызывает [starting] перед КАЖДЫМ тестом класса и [finished] после
 * (даже если тест упал). `TestWatcher` – готовая основа для таких правил.
 * - `Dispatchers.setMain(testDispatcher)` – «вместо Main используйте этот тестовый диспетчер»;
 * - `Dispatchers.resetMain()` – вернуть как было, чтобы подмена не «протекла» в другие тесты.
 *
 * [StandardTestDispatcher] работает на **виртуальном времени**: `delay(5000)` проходит мгновенно,
 * а запущенные корутины не выполняются сами – их «прокручивает» тест:
 * - `advanceUntilIdle()` – выполнить всё и промотать все delay;
 * - `runCurrent()` – выполнить только то, что готово прямо сейчас, без перемотки времени;
 * - `advanceTimeBy(ms)` – промотать время на ms.
 *
 * Подключение в тесте:
 * ```
 * @get:Rule                       // «повесь @Rule на геттер»: JUnit (Java) ищет публичное поле или геттер,
 * val mainDispatcherRule = MainDispatcherRule()   // а у Kotlin-свойства само поле приватное
 *
 * @Test
 * fun something() = runTest(mainDispatcherRule.testDispatcher) { ... }
 * ```
 * `runTest(mainDispatcherRule.testDispatcher)` – чтобы тест и ViewModel жили на одних и тех же виртуальных часах.
 *
 * Это стандартный рецепт из документации Android: https://developer.android.com/kotlin/coroutines/test#setting-main-dispatcher
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}