package com.example.ui.viewmodel

data class WorldCity(
    val zoneId: String,
    val city: String,
    val country: String
)

object WorldClockCatalog {
    val cities = listOf(
        WorldCity("Asia/Riyadh", "Makkah / Riyadh", "Saudi Arabia"),
        WorldCity("Europe/Istanbul", "Istanbul", "Turkey"),
        WorldCity("Asia/Karachi", "Karachi", "Pakistan"),
        WorldCity("Asia/Dubai", "Dubai", "UAE"),
        WorldCity("Africa/Cairo", "Cairo", "Egypt"),
        WorldCity("Asia/Jakarta", "Jakarta", "Indonesia"),
        WorldCity("Asia/Kuala_Lumpur", "Kuala Lumpur", "Malaysia"),
        WorldCity("Asia/Kolkata", "New Delhi", "India"),
        WorldCity("Asia/Dhaka", "Dhaka", "Bangladesh"),
        WorldCity("Europe/London", "London", "United Kingdom"),
        WorldCity("Europe/Paris", "Paris", "France"),
        WorldCity("Europe/Berlin", "Berlin", "Germany"),
        WorldCity("America/New_York", "New York", "United States"),
        WorldCity("America/Chicago", "Chicago", "United States"),
        WorldCity("America/Los_Angeles", "Los Angeles", "United States"),
        WorldCity("America/Toronto", "Toronto", "Canada"),
        WorldCity("Asia/Tokyo", "Tokyo", "Japan"),
        WorldCity("Asia/Singapore", "Singapore", "Singapore"),
        WorldCity("Australia/Sydney", "Sydney", "Australia")
    )

    fun findByZone(zoneId: String): WorldCity {
        return cities.firstOrNull { it.zoneId == zoneId }
            ?: WorldCity(
                zoneId = zoneId,
                city = zoneId.substringAfterLast('/').replace('_', ' '),
                country = zoneId.substringBefore('/')
            )
    }
}
