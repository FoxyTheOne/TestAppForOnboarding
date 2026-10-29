package com.foxynest.testappforonboarding

import com.foxynest.testappforonboarding.other.extensions.calculateAge
import com.foxynest.testappforonboarding.other.extensions.parseDateToLocalDate
import com.foxynest.testappforonboarding.other.extensions.toMySaveFormatString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateConversionTests {
    @Test
    fun `save format round trip`() {
        val date = LocalDate.of(2021, 1, 1)
        assertEquals("2021-01-01", date.toMySaveFormatString())
        assertEquals(date, "2021-01-01".parseDateToLocalDate())
    }

    @Test
    fun `age is counted in full years`() {
        val birthDate = LocalDate.of(2006, 9, 15)
        assertEquals(19, birthDate.calculateAge(LocalDate.of(2026, 9, 14)).data)
        assertEquals(20, birthDate.calculateAge(LocalDate.of(2026, 9, 15)).data)
    }
}