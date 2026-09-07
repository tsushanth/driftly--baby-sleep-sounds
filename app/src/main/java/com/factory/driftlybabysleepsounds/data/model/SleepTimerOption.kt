package com.factory.driftlybabysleepsounds.data.model

enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    FIFTEEN(15, "15 min"),
    THIRTY(30, "30 min"),
    FORTY_FIVE(45, "45 min"),
    SIXTY(60, "1 hour"),
    NINETY(90, "1.5 hours"),
    ALL_NIGHT(480, "All night (8 hr)")
}
