package com.example.kursywalut

enum class RefreshInterval(val label: String, val millis: Long) {
    NEVER   ("Off",     0L),
    MIN_1   ("1 min",   1  * 60 * 1000L),
    MIN_5   ("5 min",   5  * 60 * 1000L),
    MIN_30  ("30 min",  30 * 60 * 1000L),
    HOUR_1  ("1 hour",  1  * 60 * 60 * 1000L),
    HOUR_12 ("12 hours",12 * 60 * 60 * 1000L),
    HOUR_24 ("24 hours",24 * 60 * 60 * 1000L),
}