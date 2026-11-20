package com.example.alarm

import kotlin.random.Random

object ProofPhraseGenerator {
    private val prefixes = listOf(
        "FAJR-AWAKE",
        "DAWN-RISE",
        "STAND-UP",
        "PRAY-FAJR",
        "NO-SLEEP",
        "MORNING-LIGHT",
        "AWAKE-PROOF",
        "RISE-SHARP"
    )

    fun generate(label: String = ""): String {
        val cleanLabel = label.trim().uppercase()
        val prefix = when {
            cleanLabel.contains("FAJR") -> "FAJR-AWAKE"
            cleanLabel.contains("TAHAJJUD") -> "TAHAJJUD-UP"
            cleanLabel.contains("GYM") || cleanLabel.contains("RUN") -> "MOVE-NOW"
            else -> prefixes[Random.nextInt(prefixes.size)]
        }
        val code = Random.nextInt(1000, 9999)
        return "$prefix-$code"
    }

    fun matches(input: String, expected: String): Boolean {
        val normInput = normalize(input)
        val normExpected = normalize(expected)
        return normExpected.isNotEmpty() && normInput == normExpected
    }

    private fun normalize(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .uppercase()
    }
}
