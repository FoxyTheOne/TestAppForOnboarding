package com.foxynest.testappforonboarding.domain.entities

import com.foxynest.testappforonboarding.domain.entities.onboarding.YesNoAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YesNoAnswerTest {
    @Test
    fun `legacy localized values are migrated to keys`() {
        assertEquals("YES", YesNoAnswer.migrate("Да"))
        assertEquals("YES", YesNoAnswer.migrate("Yes"))
        assertEquals("NO", YesNoAnswer.migrate("Нет"))
        assertEquals("NO", YesNoAnswer.migrate("no"))
    }

    @Test
    fun `keys and unknown values stay unchanged`() {
        assertEquals("YES", YesNoAnswer.migrate("YES"))
        assertEquals("", YesNoAnswer.migrate(""))
        assertNull(YesNoAnswer.fromStoredValue("maybe"))
    }
}