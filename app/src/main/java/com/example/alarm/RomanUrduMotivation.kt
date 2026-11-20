package com.example.alarm

import java.util.Calendar
import kotlin.random.Random

data class MotivationalMessage(
    val timePeriodTag: String,
    val greeting: String,
    val headline: String,
    val body: String
)

object RomanUrduMotivation {

    private val fajrMessages = listOf(
        MotivationalMessage(
            timePeriodTag = "FAJR • PRE-DAWN",
            greeting = "Subah Bakhair, Champion!",
            headline = "Yes! Mujhe pata tha tum kar sakte ho!",
            body = "Tumhara passion aur himmat kamaal hai — Fajr ke waqt neend ko harana har kisi ke bas ki baat nahi. Excellent dude, Allah tumhare din mein barkat de!"
        ),
        MotivationalMessage(
            timePeriodTag = "FAJR • DAWN VICTORY",
            greeting = "Waah Yaar, Zabardast!",
            headline = "I love your passion — neend haar gayi!",
            body = "Tumne aaj phir sabit kar diya ke irada pakka ho toh koi cheez tumhe rok nahi sakti. Excellent dude, isi jazbe ke saath apna din shuru karo!"
        ),
        MotivationalMessage(
            timePeriodTag = "FAJR • EARLY RISE",
            greeting = "Assalam-o-Alaikum, Rockstar!",
            headline = "Mujhe yaqeen tha tum uth jao ge!",
            body = "Subah savere uth kar tumne din ki pehli aur sabse badi jeet hasil kar li. Zabardast jazba yaar, aaj chaa jao!"
        )
    )

    private val morningMessages = listOf(
        MotivationalMessage(
            timePeriodTag = "MORNING • SUBAH",
            greeting = "Subah Bakhair, Dost!",
            headline = "Yes! I knew it tum kar sakte ho!",
            body = "Tumhari energy aur passion lajawab hai. Excellent dude — utho, muskurao aur aaj ka din apna bana lo!"
        ),
        MotivationalMessage(
            timePeriodTag = "MORNING • FRESH START",
            greeting = "Good Morning, Champ!",
            headline = "I love your passion, zabardast!",
            body = "Nayi subah, naye goals aur tumhara solid commitment. Mujhe pata tha tum bilkul waqt par utho ge — shuru karo!"
        )
    )

    private val afternoonMessages = listOf(
        MotivationalMessage(
            timePeriodTag = "AFTERNOON • DOPEHAR",
            greeting = "Dopehar Bakhair, Champ!",
            headline = "Yes! Mujhe pata tha tum kar sakte ho!",
            body = "Tumhara focus aur passion next level hai. Excellent dude, ab full energy ke saath baqi din bhi jeet lo!"
        )
    )

    private val eveningMessages = listOf(
        MotivationalMessage(
            timePeriodTag = "EVENING • SHAAM",
            greeting = "Shaam Bakhair, Superstar!",
            headline = "I love your passion — kamaal kar diya!",
            body = "Shaam ho ya subah, tumhara discipline aur jazba be-misaal hai. Excellent dude, lage raho!"
        )
    )

    private val nightMessages = listOf(
        MotivationalMessage(
            timePeriodTag = "NIGHT • RAAT",
            greeting = "Waah Champ, Lajawab!",
            headline = "Yes! Mujhe pata tha tum kar sakte ho!",
            body = "Itni raat ya late hours mein bhi tumhara passion aur commitment kamaal ka hai. Excellent dude, stay unstoppable!"
        )
    )

    fun forTime(
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        alarmLabel: String = "",
        seed: Int = Random.nextInt(1000)
    ): MotivationalMessage {
        val isFajrLabel = alarmLabel.contains("fajr", ignoreCase = true) ||
            alarmLabel.contains("tahajjud", ignoreCase = true)
        val pool = when {
            hourOfDay in 3..6 || (isFajrLabel && hourOfDay in 2..7) -> fajrMessages
            hourOfDay in 7..11 -> morningMessages
            hourOfDay in 12..16 -> afternoonMessages
            hourOfDay in 17..20 -> eveningMessages
            else -> nightMessages
        }
        return pool[kotlin.math.abs(seed) % pool.size]
    }

    fun shortGreeting(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (hourOfDay) {
            in 3..6 -> "Fajr & Dawn • Subah Bakhair"
            in 7..11 -> "Subah Bakhair"
            in 12..16 -> "Dopehar Bakhair"
            in 17..20 -> "Shaam Bakhair"
            else -> "Shab Bakhair"
        }
    }
}
