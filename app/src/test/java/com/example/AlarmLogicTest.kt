package com.example

import com.example.alarm.BuiltInAlarmTones
import com.example.alarm.ProofPhraseGenerator
import com.example.alarm.RomanUrduMotivation
import com.example.data.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmLogicTest {

    @Test
    fun proofPhraseGenerator_matchesTrimmedAndCaseInsensitive() {
        val phrase = ProofPhraseGenerator.generate("Fajr Namaz")
        assertTrue(phrase.startsWith("FAJR-AWAKE-"))
        assertTrue(ProofPhraseGenerator.matches("  ${phrase.lowercase()}  ", phrase))
        assertFalse(ProofPhraseGenerator.matches("WRONG-1234", phrase))
        assertFalse(ProofPhraseGenerator.matches("", phrase))
    }

    @Test
    fun alarmEntity_repeatSummaryFormatsCorrectly() {
        val daily = AlarmEntity(hour = 6, minute = 0, repeatDaysMask = AlarmEntity.MASK_EVERY_DAY)
        assertEquals("Every day", daily.repeatSummary())

        val weekdays = AlarmEntity(hour = 7, minute = 30, repeatDaysMask = AlarmEntity.MASK_WEEKDAYS)
        assertEquals("Weekdays", weekdays.repeatSummary())

        val once = AlarmEntity(hour = 5, minute = 15, repeatDaysMask = 0)
        assertEquals("Once", once.repeatSummary())
    }

    @Test
    fun romanUrduMotivation_adaptsToTimeOfDay() {
        val fajr = RomanUrduMotivation.forTime(hourOfDay = 5, alarmLabel = "Fajr Namaz", seed = 0)
        assertTrue(fajr.timePeriodTag.contains("FAJR"))
        assertTrue(fajr.body.isNotBlank())

        val morning = RomanUrduMotivation.forTime(hourOfDay = 9, alarmLabel = "Work", seed = 0)
        assertTrue(morning.timePeriodTag.contains("MORNING"))

        val afternoon = RomanUrduMotivation.forTime(hourOfDay = 14, alarmLabel = "Nap", seed = 0)
        assertTrue(afternoon.timePeriodTag.contains("AFTERNOON"))

        val evening = RomanUrduMotivation.forTime(hourOfDay = 18, alarmLabel = "Gym", seed = 0)
        assertTrue(evening.timePeriodTag.contains("EVENING"))

        val night = RomanUrduMotivation.forTime(hourOfDay = 23, alarmLabel = "Study", seed = 0)
        assertTrue(night.timePeriodTag.contains("NIGHT"))
    }

    @Test
    fun builtInAlarmTones_containsFiveUniqueTones() {
        assertEquals(5, BuiltInAlarmTones.allTones.size)
        assertEquals(5, BuiltInAlarmTones.allTones.map { it.uri }.distinct().size)
        assertTrue(BuiltInAlarmTones.isBuiltIn(BuiltInAlarmTones.URI_DAWN_CHIME))
        assertFalse(BuiltInAlarmTones.isBuiltIn("content://media/external/audio/1"))
    }
}
