package com.foxynest.testappforonboarding

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.foxynest.testappforonboarding.presentation.MainActivity
import com.foxynest.testappforonboarding.presentation.welcome.auth.welcome.WelcomeFragment
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

//@RunWith(AndroidJUnit4::class)
//class NavigationTests {
//    @Test
//    fun `welcome fragment navigates to question1 on button click`() {
//        val scenario = launchFragmentInContainer<WelcomeFragment>()
//
//        scenario.onFragment { fragment ->
//            // Проверяем что фрагмент создан
//            assertNotNull(fragment)
//
//            // Проверяем навигацию
//            fragment.findNavController().setGraph(R.navigation.app_navigation)
//
//            // Кликаем кнопку
//            fragment.binding?.buttonStartOnboarding?.performClick()
//
//            // Проверяем направление
//            assertEquals(
//                R.id.action_welcomeFragment_to_navigation_question1,
//                fragment.findNavController().currentDestination?.id
//            )
//        }
//    }
//}

@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationTests {

    @Test
    fun welcomeFragment_navigatesToQuestion1_onButtonClick() {
        // Запускаем MainActivity
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            // Ждем пока WelcomeFragment загрузится
            Thread.sleep(1000)

            // Нажимаем кнопку "Начать онбординг"
            Espresso.onView(ViewMatchers.withId(R.id.button_start_onboarding))
                .perform(ViewActions.click())

            // Проверяем что перешли к Question1Fragment
            // Здесь можно проверить наличие элементов Question1Fragment
            Espresso.onView(ViewMatchers.withId(R.id.text_field_user_name_answer))
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(ViewMatchers.isDisplayed()))

        } finally {
            activityScenario.close()
        }
    }

}