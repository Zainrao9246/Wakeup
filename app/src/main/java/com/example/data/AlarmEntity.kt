package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String = "Fajr Namaz",
    val isEnabled: Boolean = true,
    /** Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64. 0 = One-time */
    val repeatDaysMask: Int = 0b1111111,
    val awakeProofEnabled: Boolean = true,
    val proofPhrase: String = "",
    val reRingIntervalSeconds: Int = 60,
    /** If true, hides the phrase on the challenge screen so user must paste/enter saved text. */
    val strictHidePhrase: Boolean = true,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default Alarm",
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    /** True while user swiped/silenced/volume-pressed WITHOUT submitting Awake Proof. */
    val isPendingReRing: Boolean = false,
    val nextReRingAtMillis: Long? = null,
    val reRingAttemptCount: Int = 0,
    /** Epoch day (LocalDate.toEpochDay / millis / 86400000) when Awake Proof was verified today. */
    val lastDismissedEpochDay: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun isDaySelected(dayIndexMonZero: Int): Boolean {
        return (repeatDaysMask and (1 shl dayIndexMonZero)) != 0
    }

    fun repeatSummary(): String {
        return when (repeatDaysMask) {
            0 -> "Once"
            0b1111111 -> "Every day"
            0b0011111 -> "Weekdays"
            0b1100000 -> "Weekends"
            else -> {
                val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                names.filterIndexed { index, _ -> isDaySelected(index) }.joinToString(", ")
            }
        }
    }

    companion object {
        const val MASK_EVERY_DAY = 0b1111111
        const val MASK_WEEKDAYS = 0b0011111
        const val MASK_WEEKENDS = 0b1100000

        fun calendarDayToMonZeroIndex(calendarDayOfWeek: Int): Int {
            return when (calendarDayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
        }
    }
}
